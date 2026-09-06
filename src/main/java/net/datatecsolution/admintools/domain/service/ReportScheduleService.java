package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.ReportScheduleCRUD;
import net.datatecsolution.admintools.persistence.crud.ReportSendLogCRUD;
import net.datatecsolution.admintools.persistence.entity.ReportSchedule;
import net.datatecsolution.admintools.persistence.entity.ReportSendLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * US-068 — programación de envíos: CRUD de {@code report_schedules} y el
 * ciclo del scheduler (cada minuto, en la zona del negocio):
 *
 * una programación ACTIVA dispara cuando su hora ya pasó HOY (y el día de la
 * semana coincide, si es SEMANAL) y todavía no tiene registro AUTO para hoy —
 * eso hace el ciclo idempotente ante reinicios y también recupera envíos si
 * la API estuvo caída a la hora exacta (se envía al volver, mismo día).
 */
@Service
public class ReportScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleService.class);
    static final Set<String> REPORTES = Set.of("DAILY", "ABC", "ROTATION", "PURCHASE", "CATEGORY");

    private final ReportScheduleCRUD scheduleCRUD;
    private final ReportSendLogCRUD logCRUD;
    private final ReportMailService mailService;

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    public ReportScheduleService(ReportScheduleCRUD scheduleCRUD, ReportSendLogCRUD logCRUD,
                                 ReportMailService mailService) {
        this.scheduleCRUD = scheduleCRUD;
        this.logCRUD = logCRUD;
        this.mailService = mailService;
    }

    /* ---------- CRUD ---------- */

    public List<ReportSchedule> listar() {
        return scheduleCRUD.findAllByOrderByIdAsc();
    }

    public ReportSchedule guardar(ReportSchedule s) {
        validar(s);
        return scheduleCRUD.save(s);
    }

    public ReportSchedule obtener(int id) {
        return scheduleCRUD.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programación no encontrada"));
    }

    public void eliminar(int id) {
        scheduleCRUD.delete(obtener(id));
    }

    public List<ReportSendLog> historico(int id) {
        obtener(id);
        return logCRUD.findTop30ByScheduleIdOrderByIdDesc(id);
    }

    private void validar(ReportSchedule s) {
        if (s.getReporte() == null || !REPORTES.contains(s.getReporte())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reporte debe ser uno de " + REPORTES);
        }
        boolean semanal = "SEMANAL".equals(s.getFrecuencia());
        if (!semanal && !"DIARIA".equals(s.getFrecuencia())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frecuencia debe ser DIARIA o SEMANAL");
        }
        if (semanal && (s.getDiaSemana() == null || s.getDiaSemana() < 1 || s.getDiaSemana() > 7)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "diaSemana (1=lunes..7=domingo) es obligatorio para frecuencia SEMANAL");
        }
        if (s.getHora() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hora es obligatoria");
        }
        if (s.getDestinatarios() == null || s.getDestinatarios().isBlank()
                || !s.getDestinatarios().matches("[^@,\\s]+@[^@,\\s]+(\\s*,\\s*[^@,\\s]+@[^@,\\s]+)*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "destinatarios: uno o más correos separados por coma");
        }
    }

    /* ---------- Scheduler ---------- */

    /** Lógica pura de disparo (testeable sin reloj real). */
    static boolean debeDisparar(ReportSchedule s, LocalDateTime ahora) {
        if (!Boolean.TRUE.equals(s.getActivo())) return false;
        if ("SEMANAL".equals(s.getFrecuencia())
                && (s.getDiaSemana() == null || ahora.getDayOfWeek().getValue() != s.getDiaSemana())) {
            return false;
        }
        LocalTime hora = s.getHora();
        return hora != null && !ahora.toLocalTime().isBefore(hora);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void procesarPendientes() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of(timezoneId));
        LocalDate hoy = ahora.toLocalDate();
        for (ReportSchedule s : scheduleCRUD.findByActivoTrue()) {
            if (!debeDisparar(s, ahora)) continue;
            if (logCRUD.existsByScheduleIdAndFechaProgramadaAndOrigen(s.getId(), hoy, "AUTO")) continue;
            ejecutar(s, hoy, "AUTO");
        }
    }

    /** Envío manual (botón "enviar ahora"); no consume el ciclo AUTO del día. */
    public ReportSendLog enviarAhora(int id) {
        ReportSchedule s = obtener(id);
        LocalDate hoy = LocalDate.now(ZoneId.of(timezoneId));
        return ejecutar(s, hoy, "MANUAL");
    }

    private ReportSendLog ejecutar(ReportSchedule s, LocalDate hoy, String origen) {
        ReportMailService.Resultado r = mailService.generarYEnviar(s.getReporte(), s.getDestinatarios(), hoy);
        ReportSendLog entrada = new ReportSendLog();
        entrada.setScheduleId(s.getId());
        entrada.setFechaProgramada(hoy);
        entrada.setOrigen(origen);
        entrada.setEstado(r.ok() ? "OK" : "FALLO");
        entrada.setIntentos(Math.max(r.intentos(), 1));
        entrada.setDetalle(r.detalle() != null && r.detalle().length() > 500
                ? r.detalle().substring(0, 500) : r.detalle());
        ReportSendLog guardada = logCRUD.save(entrada);
        log.info("US-068: envío {} de '{}' → {} (intentos={})", origen, s.getReporte(),
                guardada.getEstado(), guardada.getIntentos());
        return guardada;
    }
}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.ReportScheduleCRUD;
import net.datatecsolution.admintools.persistence.crud.ReportSendLogCRUD;
import net.datatecsolution.admintools.persistence.entity.ReportSchedule;
import net.datatecsolution.admintools.persistence.entity.ReportSendLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-068 — scheduler de envíos. Se prueba la lógica de disparo pura, la
 * idempotencia por histórico (un AUTO por día) y que el resultado del envío
 * (OK o FALLO tras reintentos) siempre queda registrado.
 */
@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceTest {

    @Mock private ReportScheduleCRUD scheduleCRUD;
    @Mock private ReportSendLogCRUD logCRUD;
    @Mock private ReportMailService mailService;

    private ReportScheduleService service;

    // lunes 7-sep-2026, 08:30 hora del negocio
    private static final LocalDateTime LUNES_0830 = LocalDateTime.of(2026, 9, 7, 8, 30);

    @BeforeEach
    void setUp() {
        service = new ReportScheduleService(scheduleCRUD, logCRUD, mailService);
        ReflectionTestUtils.setField(service, "timezoneId", "America/Tegucigalpa");
    }

    private ReportSchedule sched(String frecuencia, Integer dia, String hora) {
        ReportSchedule s = new ReportSchedule();
        s.setId(1);
        s.setReporte("DAILY");
        s.setFrecuencia(frecuencia);
        s.setDiaSemana(dia);
        s.setHora(LocalTime.parse(hora));
        s.setDestinatarios("dueno@urbina.hn");
        s.setActivo(true);
        return s;
    }

    /* ---------- lógica de disparo ---------- */

    @Test
    void diariaDisparaCuandoLaHoraYaPaso() {
        assertThat(ReportScheduleService.debeDisparar(sched("DIARIA", null, "08:00"), LUNES_0830)).isTrue();
        assertThat(ReportScheduleService.debeDisparar(sched("DIARIA", null, "09:00"), LUNES_0830)).isFalse();
    }

    @Test
    void semanalRespetaElDia() {
        // lunes = 1
        assertThat(ReportScheduleService.debeDisparar(sched("SEMANAL", 1, "08:00"), LUNES_0830)).isTrue();
        assertThat(ReportScheduleService.debeDisparar(sched("SEMANAL", 2, "08:00"), LUNES_0830)).isFalse();
    }

    @Test
    void inactivaNoDispara() {
        ReportSchedule s = sched("DIARIA", null, "08:00");
        s.setActivo(false);
        assertThat(ReportScheduleService.debeDisparar(s, LUNES_0830)).isFalse();
    }

    /* ---------- idempotencia del ciclo ---------- */

    @Test
    void noReenviaSiYaHayRegistroAutoDeHoy() {
        ReportSchedule s = sched("DIARIA", null, "00:01");
        when(scheduleCRUD.findByActivoTrue()).thenReturn(List.of(s));
        when(logCRUD.existsByScheduleIdAndFechaProgramadaAndOrigen(eq(1), any(LocalDate.class), eq("AUTO")))
                .thenReturn(true);

        service.procesarPendientes();

        verify(mailService, never()).generarYEnviar(anyString(), anyString(), any());
    }

    @Test
    void cicloEnviaYRegistraOk() {
        ReportSchedule s = sched("DIARIA", null, "00:01");
        when(scheduleCRUD.findByActivoTrue()).thenReturn(List.of(s));
        when(logCRUD.existsByScheduleIdAndFechaProgramadaAndOrigen(eq(1), any(LocalDate.class), eq("AUTO")))
                .thenReturn(false);
        when(mailService.generarYEnviar(eq("DAILY"), eq("dueno@urbina.hn"), any()))
                .thenReturn(new ReportMailService.Resultado(true, 1, null));
        when(logCRUD.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.procesarPendientes();

        verify(logCRUD).save(org.mockito.ArgumentMatchers.argThat((ReportSendLog l) ->
                "OK".equals(l.getEstado()) && "AUTO".equals(l.getOrigen()) && l.getIntentos() == 1));
    }

    /* ---------- failover: el FALLO también queda en el histórico ---------- */

    @Test
    void falloSmtpQuedaRegistradoConIntentosYDetalle() {
        ReportSchedule s = sched("DIARIA", null, "00:01");
        when(scheduleCRUD.findById(1)).thenReturn(java.util.Optional.of(s));
        when(mailService.generarYEnviar(anyString(), anyString(), any()))
                .thenReturn(new ReportMailService.Resultado(false, 3, "Connection refused"));
        when(logCRUD.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportSendLog r = service.enviarAhora(1);

        assertThat(r.getEstado()).isEqualTo("FALLO");
        assertThat(r.getIntentos()).isEqualTo(3);
        assertThat(r.getDetalle()).contains("Connection refused");
        assertThat(r.getOrigen()).isEqualTo("MANUAL");
    }

    /* ---------- validaciones ---------- */

    @Test
    void validaReporteFrecuenciaYDestinatarios() {
        ReportSchedule malReporte = sched("DIARIA", null, "08:00");
        malReporte.setReporte("NOEXISTE");
        assertThatThrownBy(() -> service.guardar(malReporte)).isInstanceOf(ResponseStatusException.class);

        ReportSchedule semanalSinDia = sched("SEMANAL", null, "08:00");
        assertThatThrownBy(() -> service.guardar(semanalSinDia)).isInstanceOf(ResponseStatusException.class);

        ReportSchedule malCorreo = sched("DIARIA", null, "08:00");
        malCorreo.setDestinatarios("esto-no-es-un-correo");
        assertThatThrownBy(() -> service.guardar(malCorreo)).isInstanceOf(ResponseStatusException.class);

        ReportSchedule ok = sched("DIARIA", null, "08:00");
        ok.setDestinatarios("a@b.hn, c@d.hn");
        when(scheduleCRUD.save(ok)).thenReturn(ok);
        assertThat(service.guardar(ok)).isSameAs(ok);
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.service.ReportScheduleService;
import net.datatecsolution.admintools.persistence.entity.ReportSchedule;
import net.datatecsolution.admintools.persistence.entity.ReportSendLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * US-068 — programación de envíos de reportes por correo (solo ADMIN).
 */
@RestController
@RequestMapping("/reports/schedules")
@Tag(name = "Report schedules", description = "Envío programado de reportes por correo (US-068)")
@PreAuthorize("hasRole('ADMIN')")
public class ReportScheduleCtl {

    private final ReportScheduleService service;

    public ReportScheduleCtl(ReportScheduleService service) {
        this.service = service;
    }

    /** Payload del alta/edición; hora en formato HH:mm. */
    public record ScheduleInput(String reporte, String frecuencia, Integer diaSemana,
                                String hora, String destinatarios, Boolean activo) {}

    private static ReportSchedule aplicar(ReportSchedule s, ScheduleInput in) {
        s.setReporte(in.reporte());
        s.setFrecuencia(in.frecuencia());
        s.setDiaSemana("SEMANAL".equals(in.frecuencia()) ? in.diaSemana() : null);
        s.setHora(in.hora() != null ? LocalTime.parse(in.hora()) : null);
        s.setDestinatarios(in.destinatarios() != null ? in.destinatarios().trim() : null);
        s.setActivo(in.activo() == null || in.activo());
        return s;
    }

    @GetMapping
    @Operation(summary = "Listar programaciones")
    public ResponseEntity<List<ReportSchedule>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    @Operation(summary = "Crear programación")
    public ResponseEntity<ReportSchedule> crear(@RequestBody ScheduleInput in) {
        return new ResponseEntity<>(service.guardar(aplicar(new ReportSchedule(), in)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar programación")
    public ResponseEntity<ReportSchedule> editar(@PathVariable("id") int id, @RequestBody ScheduleInput in) {
        return ResponseEntity.ok(service.guardar(aplicar(service.obtener(id), in)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar programación (su histórico cae en cascada)")
    public ResponseEntity<Void> eliminar(@PathVariable("id") int id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/log")
    @Operation(summary = "Histórico de envíos (últimos 30)")
    public ResponseEntity<List<ReportSendLog>> historico(@PathVariable("id") int id) {
        return ResponseEntity.ok(service.historico(id));
    }

    @PostMapping("/{id}/send-now")
    @Operation(summary = "Enviar ahora (no consume el ciclo automático del día)")
    public ResponseEntity<ReportSendLog> enviarAhora(@PathVariable("id") int id) {
        return ResponseEntity.ok(service.enviarAhora(id));
    }
}

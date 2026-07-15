package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
import net.datatecsolution.admintools.domain.service.DailyReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * US-047 — reportes del panel admin. Por ahora solo el diario consolidado.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Reportes consolidados del panel admin (US-047)")
public class ReportCtl {

    private final DailyReportService dailyReportService;

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    public ReportCtl(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /** date default = HOY en la zona del negocio (no la del server/JVM). */
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Reporte diario: ventas, métodos de pago, tasas, descuentos y anulaciones, por caja y consolidado")
    public ResponseEntity<DailyReportResponse> daily(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "caja", required = false) Integer caja) {
        LocalDate day = date != null ? date : LocalDate.now(ZoneId.of(timezoneId));
        return ResponseEntity.ok(dailyReportService.daily(day, caja));
    }
}

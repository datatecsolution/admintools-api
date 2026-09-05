package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.AbcReportResponse;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
import net.datatecsolution.admintools.domain.dto.RotationReportResponse;
import net.datatecsolution.admintools.domain.service.AbcAnalysisService;
import net.datatecsolution.admintools.domain.service.DailyReportService;
import net.datatecsolution.admintools.domain.service.RotationReportService;
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
    private final AbcAnalysisService abcAnalysisService;
    private final RotationReportService rotationReportService;

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    public ReportCtl(DailyReportService dailyReportService, AbcAnalysisService abcAnalysisService,
                     RotationReportService rotationReportService) {
        this.dailyReportService = dailyReportService;
        this.abcAnalysisService = abcAnalysisService;
        this.rotationReportService = rotationReportService;
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

    /**
     * US-062 — análisis ABC. Defaults: último año móvil hasta hoy (zona del
     * negocio), todas las cajas, cortes 80/95, sin límite de filas.
     */
    @GetMapping("/abc")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Análisis ABC de productos: venta por artículo con participación acumulada y clase A/B/C, consolidando cajas")
    public ResponseEntity<AbcReportResponse> abc(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "caja", required = false) Integer caja,
            @RequestParam(name = "excludeCategories", required = false) java.util.List<String> excludeCategories,
            @RequestParam(name = "umbralA", required = false, defaultValue = "80") java.math.BigDecimal umbralA,
            @RequestParam(name = "umbralB", required = false, defaultValue = "95") java.math.BigDecimal umbralB,
            @RequestParam(name = "limit", required = false) Integer limit) {
        LocalDate hasta = to != null ? to : LocalDate.now(ZoneId.of(timezoneId));
        LocalDate desde = from != null ? from : hasta.minusYears(1);
        return ResponseEntity.ok(abcAnalysisService.abc(desde, hasta, caja,
                excludeCategories, umbralA, umbralB, limit));
    }

    /**
     * US-063 — rotación de inventario. Defaults: últimos 90 días hasta hoy
     * (zona del negocio), todas las cajas y bodegas, cortes 30/90 días de
     * cobertura.
     */
    @GetMapping("/rotation")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Rotación de inventario: ventas del período vs stock actual, días de cobertura y clasificación RAPIDO/MEDIO/LENTO (incluye sin movimiento)")
    public ResponseEntity<RotationReportResponse> rotation(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "caja", required = false) Integer caja,
            @RequestParam(name = "bodega", required = false) Integer bodega,
            @RequestParam(name = "excludeCategories", required = false) java.util.List<String> excludeCategories,
            @RequestParam(name = "umbralRapidoDias", required = false, defaultValue = "30") int umbralRapidoDias,
            @RequestParam(name = "umbralMedioDias", required = false, defaultValue = "90") int umbralMedioDias,
            @RequestParam(name = "limit", required = false) Integer limit) {
        LocalDate hasta = to != null ? to : LocalDate.now(ZoneId.of(timezoneId));
        LocalDate desde = from != null ? from : hasta.minusDays(89);
        return ResponseEntity.ok(rotationReportService.rotation(desde, hasta, caja, bodega,
                excludeCategories, umbralRapidoDias, umbralMedioDias, limit));
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.AbcReportResponse;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionItem;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionsResponse;
import net.datatecsolution.admintools.domain.dto.RotationReportResponse;
import net.datatecsolution.admintools.domain.service.AbcAnalysisService;
import net.datatecsolution.admintools.domain.service.DailyReportService;
import net.datatecsolution.admintools.domain.service.PurchaseSuggestionService;
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
    private final PurchaseSuggestionService purchaseSuggestionService;

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    public ReportCtl(DailyReportService dailyReportService, AbcAnalysisService abcAnalysisService,
                     RotationReportService rotationReportService,
                     PurchaseSuggestionService purchaseSuggestionService) {
        this.dailyReportService = dailyReportService;
        this.abcAnalysisService = abcAnalysisService;
        this.rotationReportService = rotationReportService;
        this.purchaseSuggestionService = purchaseSuggestionService;
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

    /**
     * US-064 — proyección de compras. Defaults: venta de los últimos 30 días
     * (zona del negocio), horizonte de 30 días, solo productos con compra
     * sugerida. format=csv descarga el listado (criterio "exportable"; el
     * Excel con branding llega con US-067).
     */
    @GetMapping("/purchase-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Proyección de compras: venta diaria promedio + stock mínimo vs stock actual → cantidad sugerida por producto con urgencia CRITICA/ALTA/NORMAL")
    public ResponseEntity<?> purchaseSuggestions(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "caja", required = false) Integer caja,
            @RequestParam(name = "bodega", required = false) Integer bodega,
            @RequestParam(name = "excludeCategories", required = false) java.util.List<String> excludeCategories,
            @RequestParam(name = "horizonteDias", required = false, defaultValue = "30") int horizonteDias,
            @RequestParam(name = "incluirTodos", required = false, defaultValue = "false") boolean incluirTodos,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "format", required = false) String format) {
        LocalDate hasta = to != null ? to : LocalDate.now(ZoneId.of(timezoneId));
        LocalDate desde = from != null ? from : hasta.minusDays(29);
        PurchaseSuggestionsResponse r = purchaseSuggestionService.suggestions(
                desde, hasta, caja, bodega, excludeCategories, horizonteDias, incluirTodos, limit);
        if ("csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"sugerencias-compra-" + hasta + ".csv\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(toCsv(r));
        }
        return ResponseEntity.ok(r);
    }

    private String toCsv(PurchaseSuggestionsResponse r) {
        StringBuilder sb = new StringBuilder(
                "codigo,articulo,categoria,unidades_periodo,venta_diaria,stock_actual,minimo,dias_cobertura,sugerido,urgencia\n");
        for (PurchaseSuggestionItem i : r.items()) {
            sb.append(i.codigoArticulo()).append(',')
              .append(csv(i.articulo())).append(',')
              .append(csv(i.categoria())).append(',')
              .append(i.unidadesPeriodo()).append(',')
              .append(i.ventaDiaria()).append(',')
              .append(i.stockActual()).append(',')
              .append(i.minimo()).append(',')
              .append(i.diasCobertura() != null ? i.diasCobertura() : "").append(',')
              .append(i.sugerido()).append(',')
              .append(i.urgencia()).append('\n');
        }
        return sb.toString();
    }

    /** Escapa un campo CSV (comillas si trae coma/comilla/salto de línea). */
    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

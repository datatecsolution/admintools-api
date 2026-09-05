package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * US-062 — respuesta de GET /reports/abc. caja=null significa consolidado de
 * todas las cajas registradas. Los umbrales son los cortes de acumulado que
 * definieron A y B (default 80/95).
 */
public record AbcReportResponse(
        LocalDate from,
        LocalDate to,
        Integer caja,
        BigDecimal umbralA,
        BigDecimal umbralB,
        BigDecimal totalVenta,
        List<AbcClassSummary> resumen,
        List<AbcItem> items
) {
}

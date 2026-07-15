package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * US-047 — reporte diario consolidado (GET /reports/daily).
 * cajas trae una fila por caja consultada; consolidado es la suma
 * (caja=0, nombre "CONSOLIDADO").
 */
public record DailyReportResponse(
        LocalDate date,
        List<CajaDailyReport> cajas,
        CajaDailyReport consolidado
) {
}

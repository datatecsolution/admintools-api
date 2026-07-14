package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-047 — fila del reporte diario: totales de UNA caja (o el consolidado,
 * con caja=0 y nombre "CONSOLIDADO"). Solo facturas ACT del día, salvo
 * anuladas/totalAnulado que cuentan las estado != 'ACT'.
 */
public record CajaDailyReport(
        int caja,
        String nombreCaja,
        long ventas,
        BigDecimal total,
        BigDecimal efectivo,
        BigDecimal tarjeta,
        BigDecimal credito,
        BigDecimal descuentos,
        BigDecimal exento,
        BigDecimal base15,
        BigDecimal base18,
        BigDecimal isv15,
        BigDecimal isv18,
        long anuladas,
        BigDecimal totalAnulado
) {
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-035 — Valor total del inventario (opcionalmente de una bodega).
 * {@code warehouse} es null cuando el total es de todas las bodegas.
 */
public record ValuationSummaryResponse(
        Integer warehouse,
        BigDecimal valorTotal
) {
}

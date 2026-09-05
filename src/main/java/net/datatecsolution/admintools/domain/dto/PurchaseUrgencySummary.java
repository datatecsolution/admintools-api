package net.datatecsolution.admintools.domain.dto;

/**
 * US-064 — resumen por urgencia de las sugerencias de compra:
 * CRITICA (stock en o bajo el mínimo), ALTA (cobertura ≤ 7 días),
 * NORMAL (el resto con compra sugerida).
 */
public record PurchaseUrgencySummary(
        String urgencia,
        int productos
) {
}

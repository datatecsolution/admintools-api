package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-063 — resumen por clasificación de rotación (RAPIDO/MEDIO/LENTO):
 * cuántos productos, cuántas unidades vendió el grupo y cuánto stock
 * inmovilizado acumula. sinMovimiento cuenta los LENTO con cero ventas.
 */
public record RotationClassSummary(
        String clasificacion,
        int productos,
        int sinMovimiento,
        BigDecimal unidades,
        BigDecimal stockActual
) {
}

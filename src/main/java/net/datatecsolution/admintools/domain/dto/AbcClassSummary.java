package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-062 — resumen por clase del análisis ABC: cuántos productos caen en la
 * clase y qué porción de la venta explican. Es el insumo directo de los
 * gráficos del dashboard (US-065/066).
 */
public record AbcClassSummary(
        String clase,
        int productos,
        BigDecimal venta,
        BigDecimal participacion
) {
}

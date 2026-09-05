package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-062 — una fila del análisis ABC: producto con su venta del período,
 * participación sobre el total, acumulado (sobre la lista ordenada por venta
 * descendente) y clase resultante (A/B/C).
 */
public record AbcItem(
        int codigoArticulo,
        String articulo,
        Integer codigoCategoria,
        String categoria,
        BigDecimal unidades,
        BigDecimal venta,
        BigDecimal participacion,
        BigDecimal acumulado,
        String clase
) {
}

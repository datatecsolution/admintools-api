package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-064 — una sugerencia de compra: venta del período base, stock actual,
 * mínimo configurado (articulo_kardex.cantidad_minima) y la cantidad
 * sugerida para cubrir el horizonte sin caer bajo el mínimo.
 *
 * sugerido = max(0, ventaDiaria × horizonteDias + minimo − stockActual),
 * redondeado hacia arriba. diasCobertura null = sin ventas en el período.
 */
public record PurchaseSuggestionItem(
        int codigoArticulo,
        String articulo,
        Integer codigoCategoria,
        String categoria,
        BigDecimal unidadesPeriodo,
        BigDecimal ventaDiaria,
        BigDecimal stockActual,
        BigDecimal minimo,
        BigDecimal diasCobertura,
        BigDecimal sugerido,
        String urgencia
) {
}

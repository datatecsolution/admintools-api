package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-035 — Valoracion de inventario de un (articulo, bodega) a costo promedio.
 */
public record StockValuationResponse(
        Integer codigoArticulo,
        String articulo,
        Integer codigoBodega,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal valorTotal
) {
}

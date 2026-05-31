package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-035 — Alerta de stock minimo: existencia por debajo (o igual) del umbral.
 */
public record LowStockResponse(
        Integer codigoArticulo,
        String articulo,
        Integer codigoBodega,
        BigDecimal cantidad,
        BigDecimal cantidadMinima,
        BigDecimal faltante
) {
}

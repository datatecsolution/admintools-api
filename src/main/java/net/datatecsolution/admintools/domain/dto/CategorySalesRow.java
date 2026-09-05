package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-106 — una celda del comparativo trimestral: ventas de una categoría
 * (tabla {@code marcas}, la categoría real del negocio) en un trimestre.
 */
public record CategorySalesRow(
        int anio,
        int trimestre,
        Integer codigoCategoria,
        String categoria,
        BigDecimal unidades,
        BigDecimal venta
) {
}

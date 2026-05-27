package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * DTO de salida del subsistema de inventario (INV-1). Reporta el saldo
 * actual de un producto en una bodega especifica, leido de la tabla
 * materializada {@code existencia_articulo_bodega}.
 */
public record StockResponse(
        Integer productCode,
        Integer warehouseCode,
        String warehouseDescription,
        BigDecimal quantity
) {
}

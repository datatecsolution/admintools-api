package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * DTO de salida del subsistema de inventario (INV-1). Reporta el saldo
 * actual de un producto en una bodega especifica, leido de la tabla
 * materializada {@code existencia_articulo_bodega}.
 *
 * US-112 (Fase 0 stock reservado) — campos ADITIVOS:
 *   reserved  = cantidades en pedidos pendientes de la bodega
 *               (v_reservado_por_articulo, estado &lt; 3)
 *   available = quantity − reserved (lo que un vendedor puede comprometer)
 * quantity sigue siendo el stock FISICO (valoracion y toma fisica van
 * contra el; consumidores existentes no cambian).
 */
public record StockResponse(
        Integer productCode,
        Integer warehouseCode,
        String warehouseDescription,
        BigDecimal quantity,
        BigDecimal reserved,
        BigDecimal available
) {
}

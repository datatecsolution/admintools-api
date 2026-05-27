package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de una devolucion de compra (INV-7). Agrupa los items
 * creados en una sola operacion bajo el header de su compra original.
 */
public record PurchaseReturnResponse(
        Integer purchaseId,
        Integer warehouseCode,
        LocalDate date,
        BigDecimal totalReturned,
        List<PurchaseReturnLineResponse> items
) {
}

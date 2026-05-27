package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

public record PurchaseReturnLineResponse(
        Integer id,                // codigo_devolucion (PK)
        Integer productId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal total
) {
}

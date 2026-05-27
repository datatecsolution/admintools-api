package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseLineResponse(
        Integer id,
        Integer productId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal tax,
        BigDecimal subtotal,
        LocalDate expirationDate
) {
}

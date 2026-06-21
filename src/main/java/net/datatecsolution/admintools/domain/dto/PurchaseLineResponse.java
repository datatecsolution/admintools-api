package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseLineResponse(
        Integer id,
        Integer productId,
        String productName,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal tax,
        BigDecimal subtotal,
        LocalDate expirationDate
) {
}

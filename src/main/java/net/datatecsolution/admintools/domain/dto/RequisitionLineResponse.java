package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

public record RequisitionLineResponse(
        Integer id,
        Integer productId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal total
) {
}

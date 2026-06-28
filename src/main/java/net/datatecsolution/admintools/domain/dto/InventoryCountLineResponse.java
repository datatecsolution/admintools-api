package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

public record InventoryCountLineResponse(
        Integer id,
        Integer productId,
        BigDecimal sistema,
        BigDecimal fisico,
        BigDecimal diferencia,
        BigDecimal costo,
        String estado
) {
}

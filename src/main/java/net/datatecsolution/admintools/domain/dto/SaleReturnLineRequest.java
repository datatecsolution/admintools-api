package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Linea de devolucion de venta. quantity y price son obligatorios; el
 * resto (tax, discount, subtotal, total) si no vienen, el service los
 * calcula a partir de quantity*price (sin impuesto, sin descuento).
 */
public record SaleReturnLineRequest(
        @NotNull Integer productId,
        @NotNull @DecimalMin(value = "0.01", message = "quantity debe ser > 0") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal total
) {
}

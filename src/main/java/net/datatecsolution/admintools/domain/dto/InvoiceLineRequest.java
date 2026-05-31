package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Checkout POS — una línea del carrito a facturar. El precio viene del POS
 * (permite override, como el Swing); el % ISV se resuelve server-side por
 * {@code taxId}.
 */
public record InvoiceLineRequest(
        @NotNull @Positive Integer productId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @PositiveOrZero BigDecimal precioUnitario,
        @PositiveOrZero BigDecimal descuento,
        @NotNull Integer taxId
) {
}

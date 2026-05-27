package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Item dentro de una devolucion de compra (INV-7). Cada item, al
 * persistirse, dispara el trigger del kardex que BAJA el stock para
 * ese producto en la bodega del header.
 */
public record PurchaseReturnLineRequest(
        @NotNull(message = "productId es obligatorio")
        @Positive(message = "productId debe ser positivo")
        Integer productId,

        @NotNull(message = "quantity es obligatoria")
        @DecimalMin(value = "0.01", message = "quantity debe ser > 0")
        BigDecimal quantity,

        @NotNull(message = "price es obligatorio")
        @DecimalMin(value = "0.0", message = "price no puede ser negativo")
        BigDecimal price,

        @PositiveOrZero(message = "tax no puede ser negativo")
        BigDecimal tax,         // opcional, default 0

        @PositiveOrZero(message = "discount no puede ser negativo")
        BigDecimal discount     // opcional, default 0
) {
}

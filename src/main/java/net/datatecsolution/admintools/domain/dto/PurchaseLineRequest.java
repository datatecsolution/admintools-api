package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linea de una compra (INV-5). Cada linea, al persistirse, dispara el
 * trigger del kardex que sube el stock para (productId, warehouse) — el
 * codigo_bodega lo asigna el service desde el header.
 */
public record PurchaseLineRequest(
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
        BigDecimal tax,         // opcional; default 0

        LocalDate expirationDate // opcional
) {
}

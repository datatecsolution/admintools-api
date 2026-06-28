package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Línea de un acta de toma física. El cliente manda lo crudo (sistema, físico,
 * costo); el service deriva diferencia y estado (faltante/sobrante/negativo/ok).
 */
public record InventoryCountLineRequest(
        @NotNull(message = "productId es obligatorio")
        @Positive(message = "productId debe ser positivo")
        Integer productId,

        @NotNull(message = "sistema es obligatorio")
        BigDecimal sistema,

        @NotNull(message = "fisico es obligatorio")
        BigDecimal fisico,

        @NotNull(message = "costo es obligatorio")
        @DecimalMin(value = "0.0", message = "costo no puede ser negativo")
        BigDecimal costo
) {
}

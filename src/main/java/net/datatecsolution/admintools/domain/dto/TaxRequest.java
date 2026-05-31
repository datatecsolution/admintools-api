package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * US-032 — Alta/edicion de un impuesto. El porcentaje se guarda como string
 * en la columna legacy {@code porcentaje}.
 */
public record TaxRequest(
        @NotBlank(message = "la descripcion es obligatoria")
        String description,
        @NotNull(message = "el porcentaje es obligatorio")
        @PositiveOrZero(message = "el porcentaje no puede ser negativo")
        BigDecimal percent
) {
}

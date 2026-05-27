package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar el master de un producto (INV-4).
 * Se usa para POST /products y PUT /products/{id}.
 * El id lo asigna la BD en POST; en PUT viene en el path.
 */
public record ProductRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
        String name,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal price,

        @NotNull(message = "La categoria es obligatoria")
        Integer categoryId,

        @NotNull(message = "El impuesto es obligatorio")
        Integer taxId,

        @PositiveOrZero(message = "El codigo alternativo debe ser >= 0")
        Integer altCode,

        @NotNull
        @Min(value = 1, message = "El tipo debe ser 1 (bien) o 2 (insumo)")
        Integer type,

        @NotNull(message = "El estado activo/inactivo es obligatorio")
        Boolean active
) {
}

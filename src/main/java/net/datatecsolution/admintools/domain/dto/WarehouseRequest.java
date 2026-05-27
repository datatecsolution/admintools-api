package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear o actualizar una bodega (INV-3).
 * El codigo lo asigna el sistema (max(MAX(bodega), MAX(departamento))+1)
 * para mantener el espejo con departamento.
 */
public record WarehouseRequest(
        @NotBlank(message = "El nombre de la bodega es obligatorio")
        @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
        String description
) {
}

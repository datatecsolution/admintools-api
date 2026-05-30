package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sprint 4.5 fix — payload para crear/editar un tipo de precio.
 *
 * El campo de la tabla `precios` se llama `descripcion`; en el DTO
 * usamos `name` por consistencia con el resto de la API (CategoryRequest
 * tambien expone `name` aunque la columna sea `descripcion`).
 */
public record PriceTypeRequest(
        @NotBlank(message = "name requerido")
        @Size(max = 255, message = "name maximo 255 caracteres")
        String name
) {}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sprint 4 #50 — payload para crear/actualizar una categoria de producto.
 * Mapea a {@code marcas.descripcion} (name) y {@code marcas.observacion}
 * (description) — la entity historicamente se llama Categoria pero la
 * tabla es {@code marcas}.
 */
public record CategoryRequest(
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 145, message = "El nombre no puede exceder 145 caracteres")
        String name,

        @Size(max = 145, message = "La descripcion no puede exceder 145 caracteres")
        String description,

        /** ¿Mostrar/seleccionar esta categoría en el POS táctil? (null = false). */
        Boolean posVisible,

        /** US-081: id de la categoría padre (null = raíz). */
        Integer parentId
) {
}

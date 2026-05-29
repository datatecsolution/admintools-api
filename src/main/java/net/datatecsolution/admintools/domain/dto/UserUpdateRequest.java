package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sprint 4 #51 — payload para actualizar un usuario existente
 * (PUT /users/{id}). NO incluye {@code username} (no se permite cambiar)
 * ni {@code password} (reset por endpoint dedicado POST /users/{id}/password).
 * NO incluye {@code enabled} (soft-delete via DELETE /users/{id}).
 */
public record UserUpdateRequest(
        @Size(max = 255)
        String nombreCompleto,

        @NotNull(message = "tipoPermiso requerido (1-4)")
        @Min(value = 1) @Max(value = 4)
        Integer tipoPermiso,

        Integer codigoCaja,
        Integer codigoEmpleado
) {
}

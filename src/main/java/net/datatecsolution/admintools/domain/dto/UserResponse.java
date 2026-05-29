package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDateTime;

/**
 * Sprint 4 #51 — respuesta JSON de un usuario del sistema. NUNCA incluye
 * el password hash. {@code role} se computa desde {@code tipoPermiso}
 * para que el frontend lo muestre en lenguaje humano sin reproducir el
 * mapeo.
 */
public record UserResponse(
        Integer id,
        String username,
        String nombreCompleto,
        Integer tipoPermiso,
        String role,
        Integer codigoCaja,
        Integer codigoEmpleado,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

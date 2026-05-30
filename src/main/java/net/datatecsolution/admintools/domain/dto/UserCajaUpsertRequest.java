package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Sprint 4.5 fix — una asignacion en el PUT /users/{id}/cajas.
 *
 * El service valida que exactamente UNA fila tenga isDefault=true.
 */
public record UserCajaUpsertRequest(
        @NotNull(message = "cajaId requerido")
        Integer cajaId,

        boolean isDefault
) {}

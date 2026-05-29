package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sprint 4 #51 — payload para reset de password de un usuario
 * (POST /users/{id}/password). El password viaja en claro por HTTPS y
 * se hashea con BCrypt antes de persistir.
 */
public record PasswordResetRequest(
        @NotBlank(message = "newPassword requerido")
        @Size(min = 4, max = 100, message = "Password entre 4 y 100 caracteres")
        String newPassword
) {
}

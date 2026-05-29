package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sprint 4 #51 — payload para crear un usuario del sistema. Usado por
 * POST /users (ADMIN). El password se hashea con BCrypt en el service
 * antes de persistir; nunca se devuelve en ningun GET.
 *
 * tipoPermiso (1-4) es la fuente unica de verdad de rol (ver retro INV §4 D7):
 *   1 = supervisor (ROLE_INVENTORY)
 *   2 = cajero     (ROLE_CASHIER)
 *   3 = vendedor   (ROLE_SELLER)
 *   4 = root       (ROLE_ADMIN)
 */
public record UserCreateRequest(
        @NotBlank(message = "Username requerido")
        @Size(max = 255)
        String username,

        @NotBlank(message = "Password requerido")
        @Size(min = 4, max = 100, message = "Password entre 4 y 100 caracteres")
        String password,

        @Size(max = 255)
        String nombreCompleto,

        @NotNull(message = "tipoPermiso requerido (1-4)")
        @Min(value = 1, message = "tipoPermiso debe ser 1-4")
        @Max(value = 4, message = "tipoPermiso debe ser 1-4")
        Integer tipoPermiso,

        Integer codigoCaja,
        Integer codigoEmpleado
) {
}

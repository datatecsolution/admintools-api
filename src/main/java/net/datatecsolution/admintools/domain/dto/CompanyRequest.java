package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * US-031 — Actualizacion de datos de la empresa. El RTN hondureno son 14
 * digitos; se valida el formato (se acepta vacio para no bloquear setups
 * iniciales que aun no lo tienen).
 */
public record CompanyRequest(
        @NotBlank(message = "el nombre es obligatorio")
        String nombre,
        @Pattern(regexp = "^$|^[0-9]{14}$", message = "el RTN debe tener 14 digitos")
        String rtn,
        String telefono,
        String correo,
        String propietario,
        String direccion,
        String logoUrl
) {
}

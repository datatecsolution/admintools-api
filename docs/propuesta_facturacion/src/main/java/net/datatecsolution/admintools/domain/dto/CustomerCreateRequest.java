package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar Cliente. No incluye id (lo genera la BD).
 * La validacion se aplica con @Valid en el controller y los errores los formatea
 * {@code GlobalExceptionHandler}.
 */
public record CustomerCreateRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String name,

        @Pattern(regexp = "^(CF|\\d{14})$", message = "El RTN debe ser 'CF' o 14 digitos")
        String rtn,

        @Size(max = 255, message = "La direccion no puede exceder 255 caracteres")
        String address,

        @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
        String phone
) {
}

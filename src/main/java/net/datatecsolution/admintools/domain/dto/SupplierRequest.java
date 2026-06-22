package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear/actualizar un proveedor (US-096).
 * Espeja el alta/edición del Swing (ViewCrearProveedor / ProveedorDao):
 * tabla {@code proveedor} con nombre_proveedor + telefono + celular + direccion.
 * El {@code saldo} NO se setea aquí: es derivado (f_saldo_proveedor).
 */
public record SupplierRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
        String name,

        @Size(max = 45, message = "El teléfono no puede exceder 45 caracteres")
        String phone,

        @Size(max = 45, message = "El celular no puede exceder 45 caracteres")
        String mobile,

        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
        String address
) {
}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * US-032 — Alta/edicion de un metodo de pago.
 */
public record PaymentMethodRequest(
        @NotBlank(message = "la descripcion es obligatoria")
        String descripcion
) {
}

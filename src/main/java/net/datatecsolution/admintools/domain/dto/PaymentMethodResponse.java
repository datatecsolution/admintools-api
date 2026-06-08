package net.datatecsolution.admintools.domain.dto;

/**
 * US-032 — Un metodo de pago del catalogo.
 */
public record PaymentMethodResponse(
        Integer id,
        String descripcion
) {
}

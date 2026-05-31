package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout POS — crear factura directo desde el carrito (sin orden).
 * {@code tipoFactura}: 1=contado, 2=crédito. Para crédito, {@code customerId}
 * debe ser un cliente real (>0).
 */
public record InvoiceCreateRequest(
        @NotNull Integer customerId,
        @NotNull Integer tipoFactura,
        Integer tipoPago,
        Integer vendedorId,
        BigDecimal cobroEfectivo,
        BigDecimal cobroTarjeta,
        String observacion,
        @NotEmpty(message = "la factura debe tener al menos una línea")
        @Valid
        List<InvoiceLineRequest> lines
) {
}

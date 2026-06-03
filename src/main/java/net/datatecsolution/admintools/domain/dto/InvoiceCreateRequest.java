package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout POS — crear factura directo desde el carrito (sin orden).
 * {@code tipoFactura}: 1=contado, 2=crédito.
 *
 * Cliente: si {@code customerId} es válido (>0) se factura a ese cliente; si no
 * y viene {@code customerName}, se registra un cliente <b>contado (tipo 1)</b>
 * al vuelo (fiel al Swing {@code registrarClienteContado}) y se factura a él. Si
 * no hay ninguno, cae a Consumidor final (id 1). El <b>crédito exige un cliente
 * gestionado (tipo 2)</b>; contado/escritos (tipo 1) no pueden recibir crédito.
 */
public record InvoiceCreateRequest(
        Integer customerId,
        String customerName,
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

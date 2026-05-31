package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-033 — Estado por factura de un cliente: saldo pendiente de cada factura
 * a credito y sus dias de antiguedad.
 */
public record InvoiceAccountResponse(
        Integer codigoCuenta,
        Integer noFactura,
        Integer codigoCaja,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        BigDecimal saldo,
        Integer diasAtraso
) {
}

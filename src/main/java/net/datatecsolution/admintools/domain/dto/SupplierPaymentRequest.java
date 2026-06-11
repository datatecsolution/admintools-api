package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Pago a proveedor: monto único contra el saldo + forma de pago + concepto. */
public record SupplierPaymentRequest(
        @NotNull(message = "el monto es obligatorio")
        @Positive(message = "el monto debe ser mayor que 0")
        BigDecimal monto,
        @NotNull(message = "la forma de pago es obligatoria")
        Integer formaPagoId,
        String concepto
) {
}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * US-033 — Solicitud de abono (pago) a nivel cliente. {@code concepto} y
 * {@code ref} son opcionales (el service aplica defaults estilo Swing).
 */
public record AbonoRequest(
        @NotNull(message = "el monto es obligatorio")
        @Positive(message = "el monto debe ser mayor que 0")
        BigDecimal monto,
        String concepto,
        String ref
) {
}

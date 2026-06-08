package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-033 — Recibo de pago resultante de un abono (o del historial).
 */
public record ReceiptResponse(
        Integer noRecibo,
        LocalDateTime fecha,
        Integer customerId,
        BigDecimal total,
        String concepto,
        String ref,
        String usuario,
        BigDecimal saldoAnterior,
        BigDecimal saldo
) {
}

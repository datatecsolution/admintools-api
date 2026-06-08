package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-033 — Un movimiento del estado de cuenta a nivel cliente
 * (fila de cuentas_por_cobrar).
 */
public record LedgerEntryResponse(
        Integer id,
        LocalDate fecha,
        String descripcion,
        BigDecimal credito,
        BigDecimal debito,
        BigDecimal saldo
) {
}

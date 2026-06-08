package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-033 — Saldo actual de cuentas por cobrar de un cliente.
 * {@code saldo} es la fuente de verdad (ultimo movimiento de
 * cuentas_por_cobrar = funcion f_saldo_cliente), no la columna legacy
 * {@code cliente.saldo}. {@code disponible = limiteCredito - saldo}.
 */
public record BalanceResponse(
        Integer customerId,
        String name,
        BigDecimal limiteCredito,
        BigDecimal saldo,
        BigDecimal disponible
) {
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/** Proveedor con saldo pendiente (cuenta por pagar) para el pago táctil. */
public record SupplierBalanceResponse(
        Integer id,
        String name,
        BigDecimal saldo
) {
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * DTO de proveedor (INV-5 + US-096). El {@code balance} es el saldo derivado
 * (f_saldo_proveedor) y solo se rellena en los endpoints de gestión
 * (search/detail/writes); en el listado plano para dropdowns va {@code null}.
 */
public record SupplierResponse(
        Integer id,
        String name,
        String phone,
        String mobile,
        String address,
        BigDecimal balance
) {
}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Crear una devolucion de venta (Sale Return). Acepta multiples items en
 * un solo POST — todos van a la misma factura. La caja se infiere del
 * usuario logueado (TenantContext, US-017).
 */
public record SaleReturnRequest(
        @NotNull Integer invoiceNumber,
        @NotEmpty(message = "items no puede estar vacio") @Valid List<SaleReturnLineRequest> items
) {
}

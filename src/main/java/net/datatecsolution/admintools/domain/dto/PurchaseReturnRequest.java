package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * Request para devolver mercaderia de UNA compra original a su proveedor
 * (INV-7). Puede contener varios items en una sola transaccion; cada item
 * dispara el trigger del kardex que baja el stock.
 */
public record PurchaseReturnRequest(
        @NotNull(message = "purchaseId es obligatorio (la compra original)")
        @Positive(message = "purchaseId debe ser positivo")
        Integer purchaseId,

        @NotNull(message = "warehouseCode es obligatorio")
        @Positive(message = "warehouseCode debe ser positivo")
        Integer warehouseCode,

        LocalDate date,    // opcional; default = hoy

        @NotEmpty(message = "la devolucion debe tener al menos un item")
        @Valid
        List<PurchaseReturnLineRequest> items
) {
}

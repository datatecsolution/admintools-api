package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Anulación TOTAL de una factura de compra: marca la compra NULA, devuelve
 * todas las líneas al kardex (baja el stock que la compra subió) y, si es a
 * crédito, reversa la cuenta por pagar. Requiere motivo y clave de supervisor.
 * Mirror de {@code CtlFacturasCompra.anulacion} del Swing.
 */
public record AnnulPurchaseRequest(
        @NotBlank(message = "el motivo es obligatorio")
        String motivo,
        @NotBlank(message = "la clave de supervisor es obligatoria")
        String supervisorPassword
) {
}

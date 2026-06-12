package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anulación de factura (US-041): total (todas las líneas restantes) o parcial
 * (cantidades por línea). Requiere motivo y clave de supervisor.
 */
public record AnnulInvoiceRequest(
        boolean total,
        List<AnnulLine> lineas,
        @NotBlank(message = "el motivo es obligatorio")
        String motivo,
        @NotBlank(message = "la clave de supervisor es obligatoria")
        String supervisorPassword
) {
    public record AnnulLine(
            @NotNull Integer codigoArticulo,
            @NotNull BigDecimal cantidad
    ) {}
}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request para registrar un acta de toma física (encabezado + detalle). Los
 * conteos y valores del encabezado los DERIVA el service a partir de las
 * líneas — el cliente no los manda (single source of truth).
 */
public record InventoryCountRequest(
        @NotNull(message = "warehouseCode es obligatorio")
        @Positive(message = "warehouseCode debe ser positivo")
        Integer warehouseCode,

        @Size(max = 255, message = "motivo no puede exceder 255 caracteres")
        String motivo,

        LocalDateTime date, // opcional; default = NOW

        @NotEmpty(message = "el acta debe tener al menos una línea contada")
        @Valid
        List<InventoryCountLineRequest> lines
) {
}

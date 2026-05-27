package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request para crear una requisicion (INV-6). Modela un traslado
 * origen → destino. Caso comun: merma de bodega de trabajo a "Perdidas".
 */
public record RequisitionRequest(
        @NotNull(message = "originWarehouseCode es obligatorio")
        @Positive(message = "originWarehouseCode debe ser positivo")
        Integer originWarehouseCode,

        @NotNull(message = "destinationWarehouseCode es obligatorio")
        @Positive(message = "destinationWarehouseCode debe ser positivo")
        Integer destinationWarehouseCode,

        LocalDateTime date,   // opcional; default = NOW

        @NotEmpty(message = "la requisicion debe tener al menos una linea")
        @Valid
        List<RequisitionLineRequest> lines
) {
}

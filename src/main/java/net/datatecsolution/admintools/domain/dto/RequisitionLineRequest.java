package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Linea de una requisicion (INV-6). Al persistirla, el trigger
 * d_requisicion_b_insert dispara las dos puntas del kardex:
 * salida en origen + entrada en destino. El balance materializado
 * se actualiza para ambas bodegas en la misma transaccion.
 */
public record RequisitionLineRequest(
        @NotNull(message = "productId es obligatorio")
        @Positive(message = "productId debe ser positivo")
        Integer productId,

        @NotNull(message = "quantity es obligatoria")
        @DecimalMin(value = "0.01", message = "quantity debe ser > 0")
        BigDecimal quantity,

        @NotNull(message = "unitPrice es obligatorio")
        @DecimalMin(value = "0.0", message = "unitPrice no puede ser negativo")
        BigDecimal unitPrice
) {
}

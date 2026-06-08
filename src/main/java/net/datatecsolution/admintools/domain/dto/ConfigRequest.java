package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * US-031 — Actualizacion de los parametros generales (config_app).
 */
public record ConfigRequest(
        @NotNull(message = "diaVencimientoFactura es obligatorio")
        @PositiveOrZero(message = "diaVencimientoFactura no puede ser negativo")
        Integer diaVencimientoFactura,
        @NotNull(message = "interesParaFacturasVenc es obligatorio")
        @PositiveOrZero(message = "interesParaFacturasVenc no puede ser negativo")
        Integer interesParaFacturasVenc
) {
}

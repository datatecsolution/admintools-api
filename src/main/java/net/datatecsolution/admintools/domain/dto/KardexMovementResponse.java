package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-035 — Un movimiento del historial de kardex.
 */
public record KardexMovementResponse(
        Integer codigoMovimiento,
        LocalDate fecha,
        Integer tipoMovimiento,
        String tipoMovimientoDesc,
        String descripcion,
        String documento,
        BigDecimal cantidad,
        BigDecimal precioUnidad,
        BigDecimal total
) {
}

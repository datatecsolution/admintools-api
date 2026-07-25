package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-108 — Re-lectura de un movimiento de caja (entrada o salida) para
 * reimprimir su comprobante. El id NO es unico entre tipos (entradas_caja y
 * salidas_caja numeran aparte), por eso el GET exige el tipo.
 */
public record CashMovementResponse(
        Integer id,
        String tipo,
        LocalDateTime fecha,
        String concepto,
        BigDecimal monto,
        String usuario,
        String estado
) {
}

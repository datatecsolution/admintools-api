package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Confirmacion del cierre. Igual que el Swing, lo unico que se persiste del
 * arqueo es el total contado ({@code efectivo_caja}); el conteo por
 * denominacion y el esperado/diferencia que manda el POS son informativos
 * (el backend recalcula el esperado por su cuenta).
 */
public record CierreCajaRequest(
        @NotNull @DecimalMin("0.00") BigDecimal contado,
        Map<Integer, Integer> conteo,
        BigDecimal esperado,
        BigDecimal diferencia
) {}

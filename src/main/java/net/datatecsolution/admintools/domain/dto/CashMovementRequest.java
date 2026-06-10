package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Movimiento de caja del POS: entrada o salida de efectivo. Mirror de
 * {@code SalidaCajaDao.registrar}/{@code EntradaCajaDao.registrar} del Swing.
 * El POS manda motivo (select) y concepto (texto libre); se persiste el que
 * venga, priorizando concepto.
 */
public record CashMovementRequest(
        @NotBlank @Pattern(regexp = "entrada|salida") String tipo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        String motivo,
        String concepto
) {}

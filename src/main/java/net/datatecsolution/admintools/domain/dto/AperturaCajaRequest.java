package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Apertura del turno: efectivo inicial contado (puede ser 0). */
public record AperturaCajaRequest(
        @NotNull @DecimalMin("0.00") BigDecimal efectivoInicial
) {}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Sprint 4.5 fix — una fila a persistir en el upsert PUT
 * /products/{id}/prices.
 *
 * value = 0 es valido (todavia no se decidio el costo, por ejemplo).
 * value negativo NO (no tiene sentido de negocio).
 */
public record ProductPriceUpsertRequest(
        @NotNull(message = "priceTypeId requerido")
        Integer priceTypeId,

        @NotNull(message = "value requerido")
        @DecimalMin(value = "0.00", message = "value debe ser >= 0")
        BigDecimal value
) {}

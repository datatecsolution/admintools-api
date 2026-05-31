package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Sprint 4.5+ fix — un impuesto del catalogo (tabla impuesto).
 *
 * `percent` viene como BigDecimal aunque la columna en BD es varchar
 * (legacy). Se parsea en el service para que el frontend reciba un
 * numero coherente.
 */
public record TaxResponse(
        Integer id,
        String description,
        BigDecimal percent
) {}

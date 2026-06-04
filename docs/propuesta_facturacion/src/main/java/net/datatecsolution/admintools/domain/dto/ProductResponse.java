package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * DTO de salida del master de producto (INV-4). Datos maestros sin stock —
 * el stock se consulta aparte por {@code GET /inventory/stock}.
 */
public record ProductResponse(
        Integer id,
        String name,
        BigDecimal price,
        Integer categoryId,
        Integer taxId,
        Integer altCode,
        Integer type,
        Boolean active
) {
}

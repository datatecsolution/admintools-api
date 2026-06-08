package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Linea de una {@link InvoiceResponse}. Reflejo de una fila de
 * {@code detalle_factura} en la BD de la caja.
 */
public record InvoiceLineResponse(
        Integer id,
        Integer productId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal total
) {
}

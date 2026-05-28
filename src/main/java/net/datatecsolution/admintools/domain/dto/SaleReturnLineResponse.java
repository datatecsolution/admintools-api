package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linea persistida de devolucion de venta. {@code addedToKardex=1}
 * confirma que el trigger detalle_devolucion_b_inset disparo y el SP
 * crear_dev_venta_kardex termino bien.
 */
public record SaleReturnLineResponse(
        Integer id,
        Integer invoiceNumber,
        Integer cajaCode,
        Integer productId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal subtotal,
        BigDecimal total,
        LocalDate date,
        Integer addedToKardex
) {
}

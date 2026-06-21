package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Resultado de anular una factura de compra (US-041 compras).
 */
public record AnnulPurchaseResponse(
        Integer numeroCompra,
        String estadoFactura,
        BigDecimal totalDevuelto,
        boolean cxpReversada,
        BigDecimal montoCxp
) {
}

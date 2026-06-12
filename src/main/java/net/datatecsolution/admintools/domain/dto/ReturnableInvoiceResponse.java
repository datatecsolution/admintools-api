package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Factura con sus líneas devolvibles, para el modal de anulación (US-041):
 * por línea, lo facturado, lo ya devuelto y lo disponible a devolver. Incluye
 * el tipo (1 contado · 2 crédito) y el estado actual de la factura.
 */
public record ReturnableInvoiceResponse(
        Integer numeroFactura,
        Integer codigoCaja,
        String estadoFactura,
        Integer tipoFactura,
        BigDecimal total,
        List<ReturnableLine> lineas
) {
    public record ReturnableLine(
            Integer codigoArticulo,
            String articulo,
            BigDecimal precio,
            BigDecimal impuestoUnitario,
            BigDecimal facturada,
            BigDecimal devuelta,
            BigDecimal disponible
    ) {}
}

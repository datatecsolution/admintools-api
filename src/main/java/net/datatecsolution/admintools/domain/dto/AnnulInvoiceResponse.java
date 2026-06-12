package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Resultado de la anulación: qué se devolvió al kardex, si la factura quedó
 * NULA y si se reversó la CxC (solo total + crédito).
 */
public record AnnulInvoiceResponse(
        Integer numeroFactura,
        boolean total,
        String estadoFactura,
        BigDecimal totalDevuelto,
        boolean cxcReversada,
        BigDecimal abonoCxc
) {}

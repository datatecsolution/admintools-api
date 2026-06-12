package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resumen de factura para la lista admin (sección Facturas, US-041). */
public record InvoiceListItem(
        Integer numeroFactura,
        Integer codigoCaja,
        LocalDateTime fecha,
        String codigoCliente,
        String nombreCliente,
        BigDecimal total,
        String estadoFactura,
        Integer tipoFactura
) {}

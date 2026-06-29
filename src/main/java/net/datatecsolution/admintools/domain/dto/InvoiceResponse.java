package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta JSON de una factura definitiva (INV-8 / US-020). Se compone de
 * una fila de {@code encabezado_factura} + sus N {@code detalle_factura} en
 * la BD de la caja activa.
 *
 * El campo {@code tenant} es el nombre de la BD de caja (admin_tools_caja_N)
 * donde quedo persistida, util para trazabilidad y para reportes
 * cross-caja en el futuro.
 */
public record InvoiceResponse(
        Integer id,
        String tenant,
        LocalDateTime date,
        Integer customerId,
        String customerName,
        Integer sellerId,
        String user,
        String status,
        Integer invoiceType,
        Integer paymentType,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal payment,
        String observation,
        String totalLetras,
        List<InvoiceLineResponse> lines,
        FiscalInfo fiscal,
        CreditoInfo credito
) {
}

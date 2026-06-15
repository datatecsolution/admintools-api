package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle completo de una factura para la sección Facturas del panel admin
 * (US-041). A diferencia de {@link InvoiceResponse} (tenant-scoped, sirve a la
 * caja del cajero logueado), éste se consulta cross-DB sobre una caja ELEGIDA
 * por el admin/supervisor — que no tiene caja propia — igual que la lista
 * admin. Trae la metadata que pide el drawer de detalle del rediseño:
 * cliente/RTN, vendedor, cajero, totales e ítems con lo ya devuelto por línea.
 */
public record InvoiceAdminDetailResponse(
        Integer numeroFactura,
        Integer codigoCaja,
        LocalDateTime fecha,
        String estadoFactura,
        Integer tipoFactura,
        Integer tipoPago,
        String codigoCliente,
        String nombreCliente,
        String rtn,
        String nombreVendedor,
        String usuario,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal descuento,
        BigDecimal total,
        BigDecimal pago,
        BigDecimal cambio,
        String observacion,
        List<Linea> lineas
) {
    public record Linea(
            Integer codigoArticulo,
            String articulo,
            BigDecimal cantidad,
            BigDecimal precio,
            BigDecimal total,
            BigDecimal devuelta
    ) {}
}

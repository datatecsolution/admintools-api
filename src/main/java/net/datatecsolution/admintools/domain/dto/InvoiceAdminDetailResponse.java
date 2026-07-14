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
        // US-040: desglose por tasa + total en letras + bloque fiscal (ticket).
        String totalLetras,
        BigDecimal subtotalExento,
        BigDecimal subtotal15,
        BigDecimal subtotal18,
        BigDecimal isv15,
        BigDecimal isv18,
        FiscalInfo fiscal,
        CreditoInfo credito,
        List<Linea> lineas,
        /** US-100: token HMAC para la URL pública de reimpresión por QR (null = feature apagada). */
        String qrToken
) {
    public record Linea(
            Integer codigoArticulo,
            String articulo,
            BigDecimal cantidad,
            BigDecimal precio,
            BigDecimal descuento,
            BigDecimal impuesto,
            BigDecimal total,
            BigDecimal devuelta
    ) {}

    /**
     * US-049 — proyección para el endpoint PÚBLICO del QR: recorta datos que el
     * cliente final no necesita y que no conviene exponer sin auth — el login
     * del cajero ({@code usuario} → insumo de credential stuffing) y las
     * facturas restantes del rango fiscal ({@code fiscal.restantes}). El resto
     * (líneas con precio de venta, totales, CAI) es el recibo del propio cliente.
     */
    public InvoiceAdminDetailResponse toPublic() {
        FiscalInfo publicFiscal = fiscal == null ? null : new FiscalInfo(
                fiscal.numeroFiscal(), fiscal.cai(), fiscal.rangoAutorizado(),
                fiscal.fechaLimiteEmision(), null, fiscal.sucursal());
        return new InvoiceAdminDetailResponse(
                numeroFactura, codigoCaja, fecha, estadoFactura, tipoFactura, tipoPago,
                codigoCliente, nombreCliente, rtn, nombreVendedor, null,
                subtotal, impuesto, descuento, total, pago, cambio, observacion,
                totalLetras, subtotalExento, subtotal15, subtotal18, isv15, isv18,
                publicFiscal, credito, lineas, qrToken);
    }
}

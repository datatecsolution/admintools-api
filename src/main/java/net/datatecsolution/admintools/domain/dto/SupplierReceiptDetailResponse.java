package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-108 — Re-lectura de un recibo de pago a proveedor para reimprimir su
 * comprobante: los campos de {@link SupplierReceiptResponse} mas fecha,
 * usuario y nombre del proveedor (como el Jasper pago_caja, que lee
 * v_recibo_pago_proveedor).
 */
public record SupplierReceiptDetailResponse(
        Integer noRecibo,
        LocalDateTime fecha,
        Integer supplierId,
        String nombreProveedor,
        BigDecimal total,
        String formaPago,
        String concepto,
        String usuario,
        BigDecimal saldoAnterior,
        BigDecimal saldo
) {
}

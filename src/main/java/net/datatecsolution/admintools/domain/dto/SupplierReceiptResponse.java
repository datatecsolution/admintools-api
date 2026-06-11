package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/** Recibo de pago a proveedor registrado (réplica de ReciboPagoProveedor del Swing). */
public record SupplierReceiptResponse(
        Integer noRecibo,
        Integer supplierId,
        BigDecimal total,
        String formaPago,
        String concepto,
        BigDecimal saldoAnterior,
        BigDecimal saldo
) {
}

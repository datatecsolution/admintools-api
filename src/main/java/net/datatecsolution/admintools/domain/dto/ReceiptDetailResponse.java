package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-108 — Re-lectura de un recibo de cliente para reimprimir su comprobante:
 * los mismos campos de {@link ReceiptResponse} mas el nombre del cliente
 * (join que el POST no necesita porque el POS ya lo tiene en pantalla).
 */
public record ReceiptDetailResponse(
        Integer noRecibo,
        LocalDateTime fecha,
        Integer customerId,
        String nombreCliente,
        BigDecimal total,
        String concepto,
        String ref,
        String usuario,
        BigDecimal saldoAnterior,
        BigDecimal saldo
) {
}

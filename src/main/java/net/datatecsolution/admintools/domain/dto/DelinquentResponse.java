package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-033 — Fila del reporte de morosidad (por factura vencida con saldo).
 */
public record DelinquentResponse(
        Integer codigoCuenta,
        Integer noFactura,
        Integer codigoCaja,
        Integer customerId,
        String nombreCliente,
        String telefono,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        BigDecimal saldo,
        Integer diasAtraso,
        String ultimoPago,
        String cobrador
) {
}

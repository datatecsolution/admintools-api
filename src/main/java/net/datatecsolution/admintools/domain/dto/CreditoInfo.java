package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-040 (variante crédito) — datos de crédito para el ticket "FACTURA A
 * CRÉDITO". Solo se llena cuando la factura es a crédito (tipo_factura = 2);
 * en contado el campo {@code credito} del response va null.
 *
 * fechaVencimiento = encabezado_factura.fecha_vencimiento;
 * saldo/abono = CxC (cuentas_facturas + f_saldo_factura_cliente / abonos);
 * interesMora = config_app.interes_para_facturas_venc (recargo mensual %).
 */
public record CreditoInfo(
        LocalDate fechaVencimiento,
        BigDecimal saldo,
        BigDecimal abono,
        Integer interesMora
) {
}

package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-033 — Proyeccion de la cuenta por factura con su saldo y antiguedad,
 * calculados por las funciones MySQL ya existentes
 * ({@code f_saldo_factura_cliente}, {@code f_no_dias_del_ultimo_pago}).
 */
public interface InvoiceAccountView {
    Integer getCodigoCuenta();
    Integer getNoFactura();
    Integer getCodigoCaja();
    LocalDate getFecha();
    LocalDate getFechaVencimiento();
    BigDecimal getSaldo();
    Integer getNoDias();
}

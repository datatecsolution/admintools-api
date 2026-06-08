package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-033 — Proyeccion del reporte de morosidad (por factura), fiel al
 * {@code CuentaFacturaDao.buscarConSaldoReporte} del Swing: saldo y dias de
 * atraso via funciones MySQL, mas datos del cliente y del cobrador.
 */
public interface DelinquentInvoiceView {
    Integer getCodigoCuenta();
    Integer getNoFactura();
    Integer getCodigoCaja();
    Integer getCodigoCliente();
    String getNombreCliente();
    String getTelefono();
    LocalDate getFecha();
    LocalDate getFechaVencimiento();
    BigDecimal getSaldo();
    Integer getNoDias();
    String getUltimoPago();
    String getNombreVendedor();
}

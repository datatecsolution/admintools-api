package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * US-033 — Cabecera de la cuenta por cobrar a nivel factura (BD comun). Liga
 * una factura a credito con su libro de movimientos
 * ({@code cuentas_por_cobrar_facturas}, vias {@code codigo_cuenta}). La
 * factura se referencia por {@code (codigo_caja, no_factura)} sin FK al
 * tenant, por lo que esta tabla se consulta sin tocar la BD per-caja.
 *
 * En US-033 es SOLO LECTURA (estado por factura + morosidad). La creacion de
 * filas en la venta a credito es US-034.
 */
@Entity
@Table(name = "cuentas_facturas")
public class CuentaFactura {

    @Id
    @Column(name = "codigo_cuenta")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoCuenta;

    @Column(name = "codigo_cliente")
    private Integer codigoCliente;

    @Column(name = "no_factura")
    private Integer noFactura;

    @Column(name = "codigo_caja")
    private Integer codigoCaja;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    public Integer getCodigoCuenta() {
        return codigoCuenta;
    }

    public void setCodigoCuenta(Integer codigoCuenta) {
        this.codigoCuenta = codigoCuenta;
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public Integer getNoFactura() {
        return noFactura;
    }

    public void setNoFactura(Integer noFactura) {
        this.noFactura = noFactura;
    }

    public Integer getCodigoCaja() {
        return codigoCaja;
    }

    public void setCodigoCaja(Integer codigoCaja) {
        this.codigoCaja = codigoCaja;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}

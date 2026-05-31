package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-034 — Libro mayor de cuentas por cobrar a nivel FACTURA (BD comun). Cada
 * fila es un movimiento de una factura a credito (ligada via {@code codigo_cuenta}
 * a {@link CuentaFactura}). El saldo de la factura es el {@code saldo} del ultimo
 * {@code codigo_reguistro} de ese {@code codigo_cuenta} — equivale a la funcion
 * MySQL {@code f_saldo_factura_cliente(codigo_cuenta)}.
 *
 * {@code tipo_movimiento}: 1=saldo inicial, 2=pago, 3=interes moratorio.
 */
@Entity
@Table(name = "cuentas_por_cobrar_facturas")
public class CuentaPorCobrarFactura {

    @Id
    @Column(name = "codigo_reguistro")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_cuenta")
    private Integer codigoCuenta;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "descripcion")
    private String descripcion = "NA";

    @Column(name = "debito")
    private BigDecimal debito = BigDecimal.ZERO;

    @Column(name = "credito")
    private BigDecimal credito = BigDecimal.ZERO;

    @Column(name = "saldo")
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "usuario")
    private String usuario = "NA";

    @Column(name = "tipo_movimiento")
    private Integer tipoMovimiento = 1;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCodigoCuenta() {
        return codigoCuenta;
    }

    public void setCodigoCuenta(Integer codigoCuenta) {
        this.codigoCuenta = codigoCuenta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getDebito() {
        return debito;
    }

    public void setDebito(BigDecimal debito) {
        this.debito = debito;
    }

    public BigDecimal getCredito() {
        return credito;
    }

    public void setCredito(BigDecimal credito) {
        this.credito = credito;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Integer getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(Integer tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }
}

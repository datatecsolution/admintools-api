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
 * US-033 — Libro mayor de cuentas por cobrar a nivel cliente (BD comun
 * {@code admin_tools}). Cada fila es un movimiento: {@code credito} (cargo,
 * sube el saldo), {@code debito} (pago/abono, baja el saldo) y {@code saldo}
 * (corriente tras el movimiento). El saldo actual del cliente es el
 * {@code saldo} del ultimo {@code codigo_reguistro} — equivalente a la
 * funcion MySQL {@code f_saldo_cliente(codigo_cliente)} que usa el Swing.
 *
 * Nota: las columnas monetarias son {@code DECIMAL(15,2)} (migradas en V35,
 * US-071). Se mapean como BigDecimal y se redondea a 2 decimales HALF_EVEN
 * al escribir, igual que el Swing.
 */
@Entity
@Table(name = "cuentas_por_cobrar")
public class CuentaPorCobrar {

    @Id
    @Column(name = "codigo_reguistro")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "codigo_cliente")
    private Integer codigoCliente;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "debito")
    private BigDecimal debito = BigDecimal.ZERO;

    @Column(name = "credito")
    private BigDecimal credito = BigDecimal.ZERO;

    @Column(name = "saldo")
    private BigDecimal saldo = BigDecimal.ZERO;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
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
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-033 — Recibo de pago (cabecera del abono) en la BD comun. Mirror de
 * {@code ReciboPagoDao.registrar(recibo)}: al aplicar un abono se inserta un
 * recibo (con {@code saldo_anterio} y {@code saldo} = anterior - total) y, en
 * paralelo, un movimiento debito en {@link CuentaPorCobrar}.
 *
 * El nombre de columna {@code saldo_anterio} conserva el typo del esquema
 * legacy a proposito (la tabla ya existe en produccion).
 */
@Entity
@Table(name = "recibo_pago")
public class ReciboPago {

    @Id
    @Column(name = "no_recibo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer noRecibo;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "codigo_cliente")
    private Integer codigoCliente;

    @Column(name = "total_letras")
    private String totalLetras = "NA";

    @Column(name = "total")
    @JdbcTypeCode(SqlTypes.REAL)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "concepto")
    private String concepto = "NA";

    @Column(name = "usuario")
    private String usuario = "SYSTEM";

    @Column(name = "saldo_anterio")
    @JdbcTypeCode(SqlTypes.REAL)
    private BigDecimal saldoAnterior = BigDecimal.ZERO;

    @Column(name = "saldo")
    @JdbcTypeCode(SqlTypes.REAL)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "ref")
    private String ref = "NA";

    public Integer getNoRecibo() {
        return noRecibo;
    }

    public void setNoRecibo(Integer noRecibo) {
        this.noRecibo = noRecibo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getTotalLetras() {
        return totalLetras;
    }

    public void setTotalLetras(String totalLetras) {
        this.totalLetras = totalLetras;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getSaldoAnterior() {
        return saldoAnterior;
    }

    public void setSaldoAnterior(BigDecimal saldoAnterior) {
        this.saldoAnterior = saldoAnterior;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}

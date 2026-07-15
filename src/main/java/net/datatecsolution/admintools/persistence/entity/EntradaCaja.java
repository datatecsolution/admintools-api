package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada de efectivo a la caja (BD comun). Mirror de {@code EntradaCaja} +
 * {@code EntradaCajaDao.registrar} del Swing: el cierre suma las entradas del
 * usuario en el rango {@code [no_entrada_inicial, no_entrada_final]} con
 * {@code estado='ACT'}.
 */
@Entity
@Table(name = "entradas_caja")
public class EntradaCaja {

    @Id
    @Column(name = "codigo_entrada")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoEntrada;

    @Column(name = "concepto")
    private String concepto = "NA";

    @Column(name = "cantidad")
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Column(name = "usuario")
    private String usuario = "system";

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "estado")
    private String estado = "ACT";

    @Column(name = "codigo_cuenta")
    private Integer codigoCuenta = -1;

    public Integer getCodigoEntrada() { return codigoEntrada; }
    public void setCodigoEntrada(Integer codigoEntrada) { this.codigoEntrada = codigoEntrada; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getCodigoCuenta() { return codigoCuenta; }
    public void setCodigoCuenta(Integer codigoCuenta) { this.codigoCuenta = codigoCuenta; }
}

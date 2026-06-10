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
 * Salida de efectivo de la caja (BD comun). Mirror de {@code SalidaCaja} +
 * {@code SalidaCajaDao.registrar} del Swing: el cierre suma las salidas del
 * usuario en el rango {@code [no_salida_inicial, no_salida_final]} con
 * {@code estado='ACT'}.
 */
@Entity
@Table(name = "salidas_caja")
public class SalidaCaja {

    @Id
    @Column(name = "codigo_salida")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoSalida;

    @Column(name = "concepto")
    private String concepto = "NA";

    @Column(name = "cantidad")
    @JdbcTypeCode(SqlTypes.REAL)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Column(name = "usuario")
    private String usuario = "system";

    @Column(name = "fecha")
    private LocalDateTime fecha;

    /** El Swing siempre manda el empleado del login; el POS usa 1 (legacy). */
    @Column(name = "codigo_empleado")
    private Integer codigoEmpleado = 1;

    @Column(name = "estado")
    private String estado = "ACT";

    @Column(name = "codigo_cuenta")
    private Integer codigoCuenta = -1;

    public Integer getCodigoSalida() { return codigoSalida; }
    public void setCodigoSalida(Integer codigoSalida) { this.codigoSalida = codigoSalida; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Integer getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(Integer codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getCodigoCuenta() { return codigoCuenta; }
    public void setCodigoCuenta(Integer codigoCuenta) { this.codigoCuenta = codigoCuenta; }
}

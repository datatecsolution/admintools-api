package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cierre de caja (turno) en la BD comun. Mirror de {@code CierreCaja} +
 * {@code CierreCajaDao} del Swing.
 *
 * Modelo del turno: una fila por turno y usuario. La apertura
 * ({@code setCierre} del Swing) inserta la fila con {@code efectivo_inicial}
 * y los numeros iniciales (continuan los finales del turno anterior + 1);
 * el cierre ({@code actualizarCierre}) la completa con los totales y deja
 * {@code estado=0}. estado=1 -> turno abierto.
 *
 * {@code factura_inicial}/{@code factura_final} son varchar legacy y desde
 * el modelo multi-caja quedan en "0": los rangos reales por caja viven en
 * {@link CierreFacturacion}.
 */
@Entity
@Table(name = "cierre_caja")
public class CierreCaja {

    @Id
    @Column(name = "idCierre")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCierre;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_final")
    private LocalDateTime fechaFinal;

    @Column(name = "factura_inicial")
    private String facturaInicial = "0";

    @Column(name = "factura_final")
    private String facturaFinal = "0";

    @Column(name = "efectivo")
    private BigDecimal efectivo = BigDecimal.ZERO;

    @Column(name = "creditos")
    private BigDecimal creditos = BigDecimal.ZERO;

    @Column(name = "tarjeta")
    private BigDecimal tarjeta = BigDecimal.ZERO;

    @Column(name = "isv15")
    private BigDecimal isv15 = BigDecimal.ZERO;

    @Column(name = "isv18")
    private BigDecimal isv18 = BigDecimal.ZERO;

    @Column(name = "totalventa")
    private BigDecimal totalVenta = BigDecimal.ZERO;

    @Column(name = "totalimpuesto")
    private BigDecimal totalImpuesto = BigDecimal.ZERO;

    @Column(name = "usuario")
    private String usuario = "system";

    @Column(name = "efectivo_inicial")
    private BigDecimal efectivoInicial = BigDecimal.ZERO;

    /** 1 = turno abierto, 0 = cerrado. */
    @Column(name = "estado")
    private Integer estado = 1;

    @Column(name = "total_isv15")
    private BigDecimal totalIsv15 = BigDecimal.ZERO;

    @Column(name = "total_isv18")
    private BigDecimal totalIsv18 = BigDecimal.ZERO;

    @Column(name = "total_excento")
    private BigDecimal totalExcento = BigDecimal.ZERO;

    /** Efectivo esperado: inicial + ventas efectivo - salidas + entradas + cobros - pagos. */
    @Column(name = "total_efectivo")
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "no_salida_inicial")
    private Integer noSalidaInicial = 0;

    @Column(name = "no_salida_final")
    private Integer noSalidaFinal = 0;

    @Column(name = "total_salida")
    private BigDecimal totalSalida = BigDecimal.ZERO;

    @Column(name = "no_entrada_inicial")
    private Integer noEntradaInicial = 0;

    @Column(name = "no_entrada_final")
    private Integer noEntradaFinal = 0;

    @Column(name = "total_entrada")
    private BigDecimal totalEntrada = BigDecimal.ZERO;

    @Column(name = "no_cobro_inicial")
    private Integer noCobroInicial = 0;

    @Column(name = "no_cobro_final")
    private Integer noCobroFinal = 0;

    @Column(name = "total_cobro")
    private BigDecimal totalCobro = BigDecimal.ZERO;

    @Column(name = "no_pago_inicial")
    private Integer noPagoInicial = 0;

    @Column(name = "no_pago_final")
    private Integer noPagoFinal = 0;

    @Column(name = "total_pago")
    private BigDecimal totalPago = BigDecimal.ZERO;

    /** Efectivo contado en el arqueo al cerrar. */
    @Column(name = "efectivo_caja")
    private BigDecimal efectivoCaja = BigDecimal.ZERO;

    @Column(name = "turno")
    private String turno = "NA";

    public Integer getIdCierre() { return idCierre; }
    public void setIdCierre(Integer idCierre) { this.idCierre = idCierre; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFinal() { return fechaFinal; }
    public void setFechaFinal(LocalDateTime fechaFinal) { this.fechaFinal = fechaFinal; }
    public String getFacturaInicial() { return facturaInicial; }
    public void setFacturaInicial(String facturaInicial) { this.facturaInicial = facturaInicial; }
    public String getFacturaFinal() { return facturaFinal; }
    public void setFacturaFinal(String facturaFinal) { this.facturaFinal = facturaFinal; }
    public BigDecimal getEfectivo() { return efectivo; }
    public void setEfectivo(BigDecimal efectivo) { this.efectivo = efectivo; }
    public BigDecimal getCreditos() { return creditos; }
    public void setCreditos(BigDecimal creditos) { this.creditos = creditos; }
    public BigDecimal getTarjeta() { return tarjeta; }
    public void setTarjeta(BigDecimal tarjeta) { this.tarjeta = tarjeta; }
    public BigDecimal getIsv15() { return isv15; }
    public void setIsv15(BigDecimal isv15) { this.isv15 = isv15; }
    public BigDecimal getIsv18() { return isv18; }
    public void setIsv18(BigDecimal isv18) { this.isv18 = isv18; }
    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }
    public BigDecimal getTotalImpuesto() { return totalImpuesto; }
    public void setTotalImpuesto(BigDecimal totalImpuesto) { this.totalImpuesto = totalImpuesto; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public BigDecimal getEfectivoInicial() { return efectivoInicial; }
    public void setEfectivoInicial(BigDecimal efectivoInicial) { this.efectivoInicial = efectivoInicial; }
    public Integer getEstado() { return estado; }
    public void setEstado(Integer estado) { this.estado = estado; }
    public BigDecimal getTotalIsv15() { return totalIsv15; }
    public void setTotalIsv15(BigDecimal totalIsv15) { this.totalIsv15 = totalIsv15; }
    public BigDecimal getTotalIsv18() { return totalIsv18; }
    public void setTotalIsv18(BigDecimal totalIsv18) { this.totalIsv18 = totalIsv18; }
    public BigDecimal getTotalExcento() { return totalExcento; }
    public void setTotalExcento(BigDecimal totalExcento) { this.totalExcento = totalExcento; }
    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal totalEfectivo) { this.totalEfectivo = totalEfectivo; }
    public Integer getNoSalidaInicial() { return noSalidaInicial; }
    public void setNoSalidaInicial(Integer noSalidaInicial) { this.noSalidaInicial = noSalidaInicial; }
    public Integer getNoSalidaFinal() { return noSalidaFinal; }
    public void setNoSalidaFinal(Integer noSalidaFinal) { this.noSalidaFinal = noSalidaFinal; }
    public BigDecimal getTotalSalida() { return totalSalida; }
    public void setTotalSalida(BigDecimal totalSalida) { this.totalSalida = totalSalida; }
    public Integer getNoEntradaInicial() { return noEntradaInicial; }
    public void setNoEntradaInicial(Integer noEntradaInicial) { this.noEntradaInicial = noEntradaInicial; }
    public Integer getNoEntradaFinal() { return noEntradaFinal; }
    public void setNoEntradaFinal(Integer noEntradaFinal) { this.noEntradaFinal = noEntradaFinal; }
    public BigDecimal getTotalEntrada() { return totalEntrada; }
    public void setTotalEntrada(BigDecimal totalEntrada) { this.totalEntrada = totalEntrada; }
    public Integer getNoCobroInicial() { return noCobroInicial; }
    public void setNoCobroInicial(Integer noCobroInicial) { this.noCobroInicial = noCobroInicial; }
    public Integer getNoCobroFinal() { return noCobroFinal; }
    public void setNoCobroFinal(Integer noCobroFinal) { this.noCobroFinal = noCobroFinal; }
    public BigDecimal getTotalCobro() { return totalCobro; }
    public void setTotalCobro(BigDecimal totalCobro) { this.totalCobro = totalCobro; }
    public Integer getNoPagoInicial() { return noPagoInicial; }
    public void setNoPagoInicial(Integer noPagoInicial) { this.noPagoInicial = noPagoInicial; }
    public Integer getNoPagoFinal() { return noPagoFinal; }
    public void setNoPagoFinal(Integer noPagoFinal) { this.noPagoFinal = noPagoFinal; }
    public BigDecimal getTotalPago() { return totalPago; }
    public void setTotalPago(BigDecimal totalPago) { this.totalPago = totalPago; }
    public BigDecimal getEfectivoCaja() { return efectivoCaja; }
    public void setEfectivoCaja(BigDecimal efectivoCaja) { this.efectivoCaja = efectivoCaja; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
}

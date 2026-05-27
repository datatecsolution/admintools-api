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
 * Entidad writable para {@code encabezado_requisicion} (INV-6).
 * Las requisiciones modelan transferencias de stock entre bodegas
 * (origen → destino). Caso comun: merma de la bodega de trabajo a la
 * bodega "Perdidas" (creada en V17).
 */
@Entity
@Table(name = "encabezado_requisicion")
public class EncabezadoRequisicion {

    @Id
    @Column(name = "codigo_requisicion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoRequisicion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "codigo_depart_destino", nullable = false)
    private Integer codigoDepartDestino;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    @Column(name = "estado_requisicion", nullable = false, length = 45)
    private String estadoRequisicion;

    @Column(name = "codigo_depart_origen", nullable = false)
    private Integer codigoDepartOrigen;

    public Integer getCodigoRequisicion() { return codigoRequisicion; }
    public void setCodigoRequisicion(Integer codigoRequisicion) { this.codigoRequisicion = codigoRequisicion; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getCodigoDepartDestino() { return codigoDepartDestino; }
    public void setCodigoDepartDestino(Integer codigoDepartDestino) { this.codigoDepartDestino = codigoDepartDestino; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
    public String getEstadoRequisicion() { return estadoRequisicion; }
    public void setEstadoRequisicion(String estadoRequisicion) { this.estadoRequisicion = estadoRequisicion; }
    public Integer getCodigoDepartOrigen() { return codigoDepartOrigen; }
    public void setCodigoDepartOrigen(Integer codigoDepartOrigen) { this.codigoDepartOrigen = codigoDepartOrigen; }
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Línea del acta de toma física — tabla {@code inventory_count_line} (V32).
 * Guarda TODAS las líneas contadas (incluidas las cuadradas) con su sistema,
 * físico, diferencia, costo y estado (faltante/sobrante/negativo/ok). FK plana
 * a {@code inventory_count} (sin @ManyToOne, igual que detalle_requisicion).
 */
@Entity
@Table(name = "inventory_count_line")
public class InventoryCountLine {

    @Id
    @Column(name = "id_detalle_inventario_count")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetalleInventarioCount;

    @Column(name = "codigo_inventario_count", nullable = false)
    private Integer codigoInventarioCount;

    @Column(name = "codigo_articulo", nullable = false)
    private Integer codigoArticulo;

    @Column(name = "sistema", nullable = false)
    private BigDecimal sistema;

    @Column(name = "fisico", nullable = false)
    private BigDecimal fisico;

    @Column(name = "diferencia", nullable = false)
    private BigDecimal diferencia;

    @Column(name = "costo", nullable = false)
    private BigDecimal costo;

    @Column(name = "estado_linea", nullable = false, length = 10)
    private String estadoLinea;

    public Integer getIdDetalleInventarioCount() { return idDetalleInventarioCount; }
    public void setIdDetalleInventarioCount(Integer v) { this.idDetalleInventarioCount = v; }
    public Integer getCodigoInventarioCount() { return codigoInventarioCount; }
    public void setCodigoInventarioCount(Integer v) { this.codigoInventarioCount = v; }
    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer v) { this.codigoArticulo = v; }
    public BigDecimal getSistema() { return sistema; }
    public void setSistema(BigDecimal v) { this.sistema = v; }
    public BigDecimal getFisico() { return fisico; }
    public void setFisico(BigDecimal v) { this.fisico = v; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal v) { this.diferencia = v; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal v) { this.costo = v; }
    public String getEstadoLinea() { return estadoLinea; }
    public void setEstadoLinea(String v) { this.estadoLinea = v; }
}

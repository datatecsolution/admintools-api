package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Linea de una requisicion (INV-6). Insertar dispara el trigger
 * {@code d_requisicion_b_insert} que llama:
 *   - {@code crear_requisicion_salida_kardex} (origen → stock baja)
 *   - {@code crear_requisicion_entrada_kardex} (destino → stock sube)
 * ambos UPSERT-ean {@code existencia_articulo_bodega} para cada bodega.
 *
 * El PK {@code id_detalle_requisicion} fue agregado por V23
 * (originalmente la tabla no tenia PK, JPA lo requiere).
 */
@Entity
@Table(name = "detalle_requisicion")
public class DetalleRequisicion {

    @Id
    @Column(name = "id_detalle_requisicion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetalleRequisicion;

    @Column(name = "codigo_requisicion", nullable = false)
    private Integer codigoRequisicion;

    @Column(name = "codigo_articulo", nullable = false)
    private Integer codigoArticulo;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unidad", nullable = false)
    private BigDecimal precioUnidad;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "codigo_depart_origen", nullable = false)
    private Integer codigoDepartOrigen;

    @Column(name = "codigo_depart_destino", nullable = false)
    private Integer codigoDepartDestino;

    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    public Integer getIdDetalleRequisicion() { return idDetalleRequisicion; }
    public void setIdDetalleRequisicion(Integer idDetalleRequisicion) { this.idDetalleRequisicion = idDetalleRequisicion; }
    public Integer getCodigoRequisicion() { return codigoRequisicion; }
    public void setCodigoRequisicion(Integer codigoRequisicion) { this.codigoRequisicion = codigoRequisicion; }
    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnidad() { return precioUnidad; }
    public void setPrecioUnidad(BigDecimal precioUnidad) { this.precioUnidad = precioUnidad; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getCodigoDepartOrigen() { return codigoDepartOrigen; }
    public void setCodigoDepartOrigen(Integer codigoDepartOrigen) { this.codigoDepartOrigen = codigoDepartOrigen; }
    public Integer getCodigoDepartDestino() { return codigoDepartDestino; }
    public void setCodigoDepartDestino(Integer codigoDepartDestino) { this.codigoDepartDestino = codigoDepartDestino; }
    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
}

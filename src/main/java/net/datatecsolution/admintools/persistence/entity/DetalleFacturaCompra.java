package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linea de una compra (INV-5). Insertar una fila aqui dispara el trigger
 * {@code detalle_compra_b_inset} que llama {@code crear_compa_kardex}:
 * stock sube + {@code existencia_articulo_bodega} se actualiza solo.
 *
 * El API no toca el kardex directamente — el trigger lo hace todo.
 */
@Entity
@Table(name = "detalle_factura_compra")
public class DetalleFacturaCompra {

    @Id
    @Column(name = "id_detalle_compra")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetalleCompra;

    @Column(name = "numero_compra", nullable = false)
    private Integer numeroCompra;

    @Column(name = "codigo_articulo", nullable = false)
    private Integer codigoArticulo;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "impuesto", nullable = false)
    private BigDecimal impuesto;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    @Column(name = "codigo_bodega", nullable = false)
    private Integer codigoBodega;

    @Column(name = "fecha_venc")
    private LocalDate fechaVenc;

    // Relacion inversa (insertable=false porque numeroCompra es la columna real)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_compra", insertable = false, updatable = false)
    private EncabezadoFacturaCompra encabezado;

    // ---- getters / setters ----
    public Integer getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(Integer idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }
    public Integer getNumeroCompra() { return numeroCompra; }
    public void setNumeroCompra(Integer numeroCompra) { this.numeroCompra = numeroCompra; }
    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getImpuesto() { return impuesto; }
    public void setImpuesto(BigDecimal impuesto) { this.impuesto = impuesto; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }
    public LocalDate getFechaVenc() { return fechaVenc; }
    public void setFechaVenc(LocalDate fechaVenc) { this.fechaVenc = fechaVenc; }
    public EncabezadoFacturaCompra getEncabezado() { return encabezado; }
    public void setEncabezado(EncabezadoFacturaCompra encabezado) { this.encabezado = encabezado; }
}

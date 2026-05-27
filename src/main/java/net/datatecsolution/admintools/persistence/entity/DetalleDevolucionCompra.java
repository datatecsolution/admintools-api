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
 * Linea de devolucion de compra (INV-7) — devolver mercaderia a un
 * proveedor. Insertar dispara el trigger
 * {@code detalle_devolucion_compra_b_i} → {@code crear_dev_compa_kardex}
 * → stock baja en la bodega + {@code existencia_articulo_bodega} se
 * actualiza solo. El API NO toca el kardex.
 *
 * No hay tabla de "header" normalizada — cada fila es una devolucion
 * independiente que referencia la compra original via {@code numero_factura}.
 * (La tabla {@code dev_compra} es un log denormalizado para reportes; el
 * API no la usa.)
 */
@Entity
@Table(name = "detalle_devoluciones_compra")
public class DetalleDevolucionCompra {

    @Id
    @Column(name = "codigo_devolucion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoDevolucion;

    /** Numero de la compra original (encabezado_factura_compra.numero_compra). */
    @Column(name = "numero_factura", nullable = false)
    private Integer numeroFactura;

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

    @Column(name = "descuento", nullable = false)
    private BigDecimal descuento;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    @Column(name = "codigo_bodega", nullable = false)
    private Integer codigoBodega;

    public Integer getCodigoDevolucion() { return codigoDevolucion; }
    public void setCodigoDevolucion(Integer codigoDevolucion) { this.codigoDevolucion = codigoDevolucion; }
    public Integer getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(Integer numeroFactura) { this.numeroFactura = numeroFactura; }
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
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }
}

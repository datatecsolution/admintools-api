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
 * Linea de devolucion de VENTA — el cliente regresa mercaderia que se le
 * habia facturado. Vive en {@code admin_tools.detalle_devoluciones}
 * (la tabla es comun, no per-caja; el campo {@code codigo_caja} marca
 * de cual caja salio la factura original).
 *
 * Insertar dispara el trigger {@code detalle_devolucion_b_inset} →
 * {@code crear_dev_venta_kardex} (con fix V19/V20: header lock + FOR UPDATE
 * saldo + UPSERT a {@code existencia_articulo_bodega}) → el stock SUBE en
 * la bodega de la caja + balance materializado actualizado. El API NO toca
 * kardex.
 *
 * Tabla denormalizada — sin "encabezado" separado. Cada fila es una
 * devolucion atomica que referencia la factura original via
 * {@code numero_factura} y {@code codigo_caja}. Multiples devoluciones
 * parciales a la misma factura son N filas distintas.
 */
@Entity
@Table(name = "detalle_devoluciones")
public class DetalleDevolucion {

    @Id
    @Column(name = "codigo_devolucion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoDevolucion;

    /** Numero de la factura original (admin_tools_caja_N.encabezado_factura.numero_factura). */
    @Column(name = "numero_factura", nullable = false)
    private Integer numeroFactura;

    /** Caja donde se hizo la factura original (admin_tools.cajas.codigo). */
    @Column(name = "codigo_caja", nullable = false)
    private Integer codigoCaja;

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

    /** Marcado a 1 por el trigger cuando termina de procesar el kardex. */
    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    public Integer getCodigoDevolucion() { return codigoDevolucion; }
    public void setCodigoDevolucion(Integer codigoDevolucion) { this.codigoDevolucion = codigoDevolucion; }

    public Integer getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(Integer numeroFactura) { this.numeroFactura = numeroFactura; }

    public Integer getCodigoCaja() { return codigoCaja; }
    public void setCodigoCaja(Integer codigoCaja) { this.codigoCaja = codigoCaja; }

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
}

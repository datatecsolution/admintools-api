package net.datatecsolution.admintools.persistence.tenant.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Linea de la factura definitiva — fila en
 * {@code admin_tools_caja_N.detalle_factura}. Al insertar, el trigger
 * {@code detalle_factura_b_insert} (V8 caja Swing) llama
 * {@code admin_tools.crear_venta_kardex} y descuenta el stock.
 *
 * No tiene FK JPA hacia {@link EncabezadoFactura} para mantener el insert
 * directo (el padre se persiste primero, su id auto_increment se obtiene, y
 * cada linea referencia ese numero_factura crudo). Esto evita complicar el
 * tenantEntityManagerFactory con cascades cross-entity.
 */
@Entity
@Table(name = "detalle_factura")
public class DetalleFactura {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_factura")
    private Integer numeroFactura;

    @Column(name = "codigo_articulo")
    private Integer codigoArticulo;

    @Column(name = "precio")
    private BigDecimal precio;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "impuesto")
    private BigDecimal impuesto;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "codigo_barra")
    private String codigoBarra;

    @Column(name = "agrega_kardex")
    private Integer agregaKardex;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

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

    public String getCodigoBarra() { return codigoBarra; }
    public void setCodigoBarra(String codigoBarra) { this.codigoBarra = codigoBarra; }

    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
}

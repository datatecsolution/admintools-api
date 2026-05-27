package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad writable para {@code encabezado_factura_compra} (INV-5).
 * Cada insert dispara (via las lineas) el trigger
 * {@code detalle_compra_b_inset} → {@code crear_compa_kardex} →
 * stock sube + {@code existencia_articulo_bodega} se actualiza solo.
 */
@Entity
@Table(name = "encabezado_factura_compra")
public class EncabezadoFacturaCompra {

    @Id
    @Column(name = "numero_compra")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer numeroCompra;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "subtotal_excento", nullable = false)
    private BigDecimal subtotalExcento;

    @Column(name = "subtotal15", nullable = false)
    private BigDecimal subtotal15;

    @Column(name = "subtotal18", nullable = false)
    private BigDecimal subtotal18;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "impuesto", nullable = false)
    private BigDecimal impuesto;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "codigo_proveedor", nullable = false)
    private Integer codigoProveedor;

    @Column(name = "codigo", nullable = false, length = 5)
    private String codigo;

    @Column(name = "estado_factura", nullable = false, length = 25)
    private String estadoFactura;

    @Column(name = "isv18", nullable = false)
    private BigDecimal isv18;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "pago", nullable = false)
    private BigDecimal pago;

    @Column(name = "no_factura_compra", nullable = false, length = 100)
    private String noFacturaCompra;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "tipo_factura", nullable = false)
    private Integer tipoFactura;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "agrega_kardex", nullable = false)
    private Integer agregaKardex;

    @Column(name = "codigo_bodega", nullable = false)
    private Integer codigoBodega;

    // Lazy: solo se carga cuando se pide explicitamente (en GET /purchases/{id})
    @OneToMany(mappedBy = "encabezado", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    private List<DetalleFacturaCompra> lineas = new ArrayList<>();

    // EAGER porque el mapper SIEMPRE expone supplierName en PurchaseResponse;
    // ahorra el lazy-loading-fuera-de-transaccion.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "codigo_proveedor", insertable = false, updatable = false)
    private Proveedor proveedor;

    // ---- getters / setters ----
    public Integer getNumeroCompra() { return numeroCompra; }
    public void setNumeroCompra(Integer numeroCompra) { this.numeroCompra = numeroCompra; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getSubtotalExcento() { return subtotalExcento; }
    public void setSubtotalExcento(BigDecimal subtotalExcento) { this.subtotalExcento = subtotalExcento; }
    public BigDecimal getSubtotal15() { return subtotal15; }
    public void setSubtotal15(BigDecimal subtotal15) { this.subtotal15 = subtotal15; }
    public BigDecimal getSubtotal18() { return subtotal18; }
    public void setSubtotal18(BigDecimal subtotal18) { this.subtotal18 = subtotal18; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getImpuesto() { return impuesto; }
    public void setImpuesto(BigDecimal impuesto) { this.impuesto = impuesto; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Integer getCodigoProveedor() { return codigoProveedor; }
    public void setCodigoProveedor(Integer codigoProveedor) { this.codigoProveedor = codigoProveedor; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEstadoFactura() { return estadoFactura; }
    public void setEstadoFactura(String estadoFactura) { this.estadoFactura = estadoFactura; }
    public BigDecimal getIsv18() { return isv18; }
    public void setIsv18(BigDecimal isv18) { this.isv18 = isv18; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public BigDecimal getPago() { return pago; }
    public void setPago(BigDecimal pago) { this.pago = pago; }
    public String getNoFacturaCompra() { return noFacturaCompra; }
    public void setNoFacturaCompra(String noFacturaCompra) { this.noFacturaCompra = noFacturaCompra; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public Integer getTipoFactura() { return tipoFactura; }
    public void setTipoFactura(Integer tipoFactura) { this.tipoFactura = tipoFactura; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }
    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }
    public List<DetalleFacturaCompra> getLineas() { return lineas; }
    public void setLineas(List<DetalleFacturaCompra> lineas) { this.lineas = lineas; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
}

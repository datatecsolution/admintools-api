package net.datatecsolution.admintools.persistence.tenant.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factura DEFINITIVA — vive en {@code admin_tools_caja_N.encabezado_factura}
 * (BD per-caja). Distinta de {@code Orden} (que vive en
 * {@code admin_tools.encabezado_factura_temp} y es la pre-factura que crea la
 * app de pedidos).
 *
 * Se persiste via {@code tenantEntityManagerFactory} — el routing a la BD
 * correcta lo hace {@code TenantRoutingDataSource} segun el JWT del request.
 *
 * Al insertar las {@link DetalleFactura}, el trigger
 * {@code detalle_factura_b_insert} (V8 caja Swing) dispara
 * {@code admin_tools.crear_venta_kardex} y descuenta el stock automaticamente.
 */
@Entity
@Table(name = "encabezado_factura")
public class EncabezadoFactura {

    @Id
    @Column(name = "numero_factura")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer numeroFactura;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "subtotal_excento")
    private BigDecimal subtotalExcento;

    @Column(name = "subtotal15")
    private BigDecimal subtotal15;

    @Column(name = "subtotal18")
    private BigDecimal subtotal18;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "impuesto")
    private BigDecimal impuesto;

    @Column(name = "total")
    private BigDecimal total;

    /**
     * Nota: en {@code admin_tools_caja_N.encabezado_factura} la columna
     * codigo_cliente es VARCHAR(18) (en admin_tools.cliente.codigo_cliente
     * es INT). Por eso aqui es String. El service convierte segun haga falta.
     */
    @Column(name = "codigo_cliente")
    private String codigoCliente;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "estado_factura")
    private String estadoFactura;

    @Column(name = "isvOtros")
    private BigDecimal isvOtros;

    @Column(name = "isv18")
    private BigDecimal isv18;

    @Column(name = "usuario")
    private String usuario;

    @Column(name = "pago")
    private BigDecimal pago;

    @Column(name = "descuento")
    private BigDecimal descuento;

    @Column(name = "tipo_factura")
    private Integer tipoFactura;

    @Column(name = "agrega_kardex")
    private Integer agregaKardex;

    @Column(name = "tipo_pago")
    private Integer tipoPago;

    @Column(name = "observacion")
    private String observacion;

    @Column(name = "total_letras")
    private String totalLetras;

    @Column(name = "codigo_vendedor")
    private Integer codigoVendedor;

    @Column(name = "estado_pago")
    private Integer estadoPago;

    @Column(name = "cod_rango")
    private Integer codRango;

    @Column(name = "cobro_tarjeta")
    private BigDecimal cobroTarjeta;

    @Column(name = "cobro_efectivo")
    private BigDecimal cobroEfectivo;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    public Integer getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(Integer numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

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

    public String getCodigoCliente() { return codigoCliente; }
    public void setCodigoCliente(String codigoCliente) { this.codigoCliente = codigoCliente; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getEstadoFactura() { return estadoFactura; }
    public void setEstadoFactura(String estadoFactura) { this.estadoFactura = estadoFactura; }

    public BigDecimal getIsvOtros() { return isvOtros; }
    public void setIsvOtros(BigDecimal isvOtros) { this.isvOtros = isvOtros; }

    public BigDecimal getIsv18() { return isv18; }
    public void setIsv18(BigDecimal isv18) { this.isv18 = isv18; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public BigDecimal getPago() { return pago; }
    public void setPago(BigDecimal pago) { this.pago = pago; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public Integer getTipoFactura() { return tipoFactura; }
    public void setTipoFactura(Integer tipoFactura) { this.tipoFactura = tipoFactura; }

    public Integer getAgregaKardex() { return agregaKardex; }
    public void setAgregaKardex(Integer agregaKardex) { this.agregaKardex = agregaKardex; }

    public Integer getTipoPago() { return tipoPago; }
    public void setTipoPago(Integer tipoPago) { this.tipoPago = tipoPago; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getTotalLetras() { return totalLetras; }
    public void setTotalLetras(String totalLetras) { this.totalLetras = totalLetras; }

    public Integer getCodigoVendedor() { return codigoVendedor; }
    public void setCodigoVendedor(Integer codigoVendedor) { this.codigoVendedor = codigoVendedor; }

    public Integer getEstadoPago() { return estadoPago; }
    public void setEstadoPago(Integer estadoPago) { this.estadoPago = estadoPago; }

    public Integer getCodRango() { return codRango; }
    public void setCodRango(Integer codRango) { this.codRango = codRango; }

    public BigDecimal getCobroTarjeta() { return cobroTarjeta; }
    public void setCobroTarjeta(BigDecimal cobroTarjeta) { this.cobroTarjeta = cobroTarjeta; }

    public BigDecimal getCobroEfectivo() { return cobroEfectivo; }
    public void setCobroEfectivo(BigDecimal cobroEfectivo) { this.cobroEfectivo = cobroEfectivo; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}

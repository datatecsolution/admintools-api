package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Detalle de una {@link Orden} — fila en {@code detalle_factura_temp}.
 * No es detalle de factura definitiva (esa la define INV-8). El atributo
 * {@code orden} reemplaza al historico {@code factura} para reflejar la
 * realidad: esta fila pertenece a una orden, no a una factura.
 */
@Entity
@Table(name = "detalle_factura_temp")
public class DetalleOrden {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idDetalle;

	@Column(name = "cantidad")
    private BigDecimal cantidad;

	@Column(name = "impuesto")
	private BigDecimal impuesto;

	@Column(name = "total")
	private BigDecimal total;

	@Column(name = "subtotal")
	private BigDecimal subTotal;

	@Column(name = "descuento")
	private BigDecimal descuentoItem;

	@Column(name = "precio")
	private Double precioVentaItem;

	@Column(name = "codigo_articulo")
	private int codigoArt=0;

	@Transient
	private double descuento=0;

	@ManyToOne
	@JoinColumn(name = "codigo_articulo", insertable = false, updatable = false)
	private Articulo articulo;

	@Transient
	private String art="";

	@Transient
	private Integer detallePrecioId=0;

	@Transient
	private double totalVentasCosto;

	@Transient
	private double ganancia;

	@Transient
	private boolean accion;

	@ManyToOne
	@JoinColumn(name = "numero_factura", insertable = true, updatable = true)
	private Orden orden;


	public Integer getIdDetalle() {
		return idDetalle;
	}

	public void setIdDetalle(Integer id) {
		this.idDetalle = id;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal getImpuesto() {
		return impuesto;
	}

	public void setImpuesto(BigDecimal impuesto) {
		this.impuesto = impuesto;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public BigDecimal getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(BigDecimal subTotal) {
		this.subTotal = subTotal;
	}

	public BigDecimal getDescuentoItem() {
		return descuentoItem;
	}

	public void setDescuentoItem(BigDecimal descuentoItem) {
		this.descuentoItem = descuentoItem;
	}

	public Double getPrecioVentaItem() {
		return precioVentaItem;
	}

	public void setPrecioVentaItem(Double precioVentaItem) {
		this.precioVentaItem = precioVentaItem;
	}

	public int getCodigoArt() {
		return codigoArt;
	}

	public void setCodigoArt(int codigoArt) {
		this.codigoArt = codigoArt;
	}

	public double getDescuento() {
		return descuento;
	}

	public void setDescuento(double descuento) {
		this.descuento = descuento;
	}

	public int getIdFactura() {
		return orden.getIdFactura();
	}

	public Articulo getArticulo() {
		return articulo;
	}

	public void setArticulo(Articulo articulo) {
		this.articulo = articulo;
	}

	public String getArt() {
		return art;
	}

	public void setArt(String art) {
		this.art = art;
	}

	public double getTotalVentasCosto() {
		return totalVentasCosto;
	}

	public void setTotalVentasCosto(double totalVentasCosto) {
		this.totalVentasCosto = totalVentasCosto;
	}

	public double getGanancia() {
		return ganancia;
	}

	public void setGanancia(double ganancia) {
		this.ganancia = ganancia;
	}

	public boolean isAccion() {
		return accion;
	}

	public void setAccion(boolean accion) {
		this.accion = accion;
	}

	public Orden getOrden() {
		return orden;
	}

	public void setOrden(Orden orden) {
		this.orden = orden;
	}

	public Integer getDetallePrecioId() {
		int precioId=0;
		for(PrecioArticulo precio : articulo.getPrecioArticulos()){
			if(precio.getPrecioArticulo().doubleValue() == precioVentaItem){
				precioId = precio.getPrecioId();
				break;
			}

		}
		return precioId;
	}

	public void setDetallePrecioId(Integer detallePrecioId) {
		this.detallePrecioId = detallePrecioId;
	}
}

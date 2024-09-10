package net.datatecsolution.admintools.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_factura_temp")
public class DetalleFactura {

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

//	@Column(name = "numero_factura")
//	private int idFactura=1;

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
	private Factura factura;


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
		return factura.getIdFactura();
	}

//	public void setIdFactura(int idFactura) {
//		this.idFactura = idFactura;
//	}

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

	public Factura getFactura() {
		return factura;
	}

	public void setFactura(Factura factura) {
		this.factura = factura;
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

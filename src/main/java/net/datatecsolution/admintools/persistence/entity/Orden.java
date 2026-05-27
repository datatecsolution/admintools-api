package net.datatecsolution.admintools.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de venta — fila en {@code encabezado_factura_temp} (la "pre-factura"
 * que crea la app de pedidos). Se llamaba {@code Factura} historicamente
 * porque comparte estructura con la factura definitiva, pero no es una
 * factura: el inventario NO se descuenta hasta que se convierte en
 * {@code encabezado_factura} (INV-8 / US-020).
 *
 * Los atributos legacy {@code idFactura}, {@code tipoFactura}, etc. quedan
 * con ese nombre porque mapean a columnas reales del esquema; renombrarlos
 * no aporta y rompe el JSON publico.
 */
@Entity
@Table(name = "encabezado_factura_temp")
public class Orden {

	private static final Logger log = LoggerFactory.getLogger(Orden.class);

	@Id
	@Column(name = "numero_factura")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idFactura;

	@Column(name = "fecha")
	@JsonIgnore
	private LocalDateTime fecha;

	@PrePersist
	protected void onCreate() {
		this.fecha = LocalDateTime.now(); // Establece la fecha actual del servidor
	}

	@Column(name = "subtotal_excento")
	@JsonIgnore
	private BigDecimal subTotalExcento;

	@Column(name = "subtotal15")
	@JsonIgnore
	private BigDecimal subTotal15;

	@Column(name = "subtotal18")
	@JsonIgnore
	private BigDecimal subTotal18;

	@Column(name = "subtotal")
	@JsonIgnore
	private BigDecimal subTotal;

	@Column(name = "impuesto")
	@JsonIgnore
	private BigDecimal totalImpuesto;

	@Column(name = "total")
	private BigDecimal total;

	@Column(name = "codigo_cliente")
	private Integer clienteId;

	//@Column(name = "codigo")
	@Transient
	private Integer codigo;

	@Column(name = "estado")
	@JsonIgnore
	private Integer estado;

	@Column(name = "isvOtros")
	private BigDecimal isvOtros=new BigDecimal(0);

	@Column(name = "isv18")
	@JsonIgnore
	private BigDecimal totalImpuesto18;

	@Column(name = "usuario")
	private String usuario;

	@Column(name = "pago")
	@JsonIgnore
	private BigDecimal pago=new BigDecimal(0);

	@Column(name = "descuento")
	@JsonIgnore
	private BigDecimal totalDescuento=new BigDecimal(0);

	@Column(name = "tipo_factura")
	@JsonIgnore
	private Integer tipoFactura=1;

	@Column(name = "codigo_caja")
	@JsonIgnore
	private Integer codigoCaja=1;

	@Column(name = "codigo_vendedor")
	private Integer vendedorCod;


	@Transient
	private BigDecimal totalOtrosImpuesto;

	@Transient
	private String fechaVencimento=null;

	@Transient
	private int agregadoAkardex;
	@Transient
	private int tipoPago;

	@Transient
	private BigDecimal cambio;

	@Transient
	private BigDecimal cobroEfectivo;

	@Transient
	private BigDecimal cobroTarjeta;

	@Column(name = "observacion")
	private String observacion="";

	@Transient
	private boolean deseaPagar=false;

	@Transient
	private int estadoPago=0;

	@Transient
	private int codigo2=0;

	@Transient
	private BigDecimal saldo=new BigDecimal(0.0);


	@ManyToOne
	@JoinColumn(name = "codigo_cliente", insertable = false, updatable = false)
	@JsonIgnore
	private Cliente cliente;

	@OneToMany(mappedBy = "orden",orphanRemoval = true,cascade = CascadeType.ALL)
	private List<DetalleOrden> detalles=new ArrayList<DetalleOrden>();

	@ManyToOne
	@JoinColumn(name = "codigo_vendedor", insertable = false, updatable = false)
	@JsonIgnore
	private Empleado vendedor=new Empleado();


	public void setCodigoAlter(int c){
		codigo2=c;
	}
	public int getCodigoAlter(){
		return codigo2;
	}


	public void setDetalles(List<DetalleOrden> d){
		detalles=d;
	}
	public List<DetalleOrden> getDetalles(){
		return detalles;
	}

	public void resetTotales(){
		totalImpuesto=BigDecimal.ZERO;
		total=BigDecimal.ZERO;
		subTotal=BigDecimal.ZERO;
		subTotal18=BigDecimal.ZERO;
		subTotal15=BigDecimal.ZERO;
		subTotalExcento=BigDecimal.ZERO;
		totalDescuento=BigDecimal.ZERO;
		totalImpuesto18=BigDecimal.ZERO;
		totalOtrosImpuesto=BigDecimal.ZERO;
		cobroEfectivo=BigDecimal.ZERO;
		cobroTarjeta=BigDecimal.ZERO;
		pago=BigDecimal.ZERO;
		cambio=BigDecimal.ZERO;

	}
	public void calcularTotales(){

		//se establecen los totales en cero
		this.resetTotales();

		// NOTA: la fecha NO se modifica aqui. El @PrePersist arriba la setea
		// al INSERT, y en los UPDATE preservamos la fecha original de
		// creacion. Si fecha se reescribiera, ordenes editadas migrarian
		// al dia actual en el filtro getByToday y se perderia trazabilidad.

		log.debug("calcularTotales() detalles={}", getDetalles().size());

		for(int x=0; x<getDetalles().size();x++){

			DetalleOrden detalle=getDetalles().get(x);

				if(detalle.getCantidad().doubleValue()!=0){

					log.debug("calcularTotales() item cantidad={} precio={}",
							detalle.getCantidad().doubleValue(), detalle.getPrecioVentaItem());


					//se obtien la cantidad y el precio de compra por unidad
					BigDecimal cantidad=detalle.getCantidad();
					BigDecimal precioVenta= new BigDecimal(detalle.getPrecioVentaItem());

					//se calcula el total del item
					BigDecimal totalItem=cantidad.multiply(precioVenta);

					BigDecimal des =detalle.getDescuentoItem();

					totalItem=totalItem.subtract(des.setScale(2, BigDecimal.ROUND_HALF_EVEN));
					//int desc=detalle.getDescuento();

					//se obtiene el impuesto del articulo
					BigDecimal porcentaImpuesto =new BigDecimal(detalle.getArticulo().getImp().getPorcentaje());
					BigDecimal porImpuesto=new BigDecimal(0);
					porImpuesto=porcentaImpuesto.divide(new BigDecimal(100));
					porImpuesto=porImpuesto.add(new BigDecimal(1));



					//se calcula el total sin  el impuesto;
					BigDecimal totalsiniva= new BigDecimal("0.0");
					totalsiniva=totalItem.divide(porImpuesto,2,BigDecimal.ROUND_HALF_EVEN);


					//se calcula el total de impuesto del item
					BigDecimal impuestoItem=totalItem.subtract(totalsiniva);



					//se estable el total y impuesto en el modelo
					setTotal(getTotal().add(totalItem.setScale(2, BigDecimal.ROUND_HALF_EVEN)));

					if(porcentaImpuesto.intValue()==0){
						setSubTotalExcento(getSubTotalExcento().add(totalsiniva.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
					}else
					if(porcentaImpuesto.intValue()==15){
						setTotalImpuesto(getTotalImpuesto().add(impuestoItem.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
						setSubTotal15(getSubTotal15().add(totalsiniva.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
					}else
					if(porcentaImpuesto.intValue()==18){
						setTotalImpuesto18(getTotalImpuesto18().add(impuestoItem.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
						setSubTotal18(getSubTotal18().add(totalsiniva.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
					}

					//se calcuala el total del impuesto de los articulo que son servicios de turismo
					if(detalle.getArticulo().getTipoArticulo()==3){
						BigDecimal totalOtrosImp= new BigDecimal("0.0");

						totalOtrosImp=totalsiniva.multiply(new BigDecimal(0.04));

						setTotalOtrosImpuesto(getTotalOtrosImpuesto().add(totalOtrosImp.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
						setTotal(getTotal().add(totalOtrosImp.setScale(2, BigDecimal.ROUND_HALF_EVEN)));

					}

					setSubTotal(getSubTotal().add(totalsiniva.setScale(2, BigDecimal.ROUND_HALF_EVEN)));
					setTotalDescuento(getTotalDescuento().add(detalle.getDescuentoItem().setScale(2, BigDecimal.ROUND_HALF_EVEN)));

					detalle.setSubTotal(totalsiniva.setScale(2, BigDecimal.ROUND_HALF_EVEN));
					detalle.setImpuesto(impuestoItem.setScale(2, BigDecimal.ROUND_HALF_EVEN));

					//se establece en la y el impuesto en el item de la vista
					detalle.setTotal(totalItem.setScale(2, BigDecimal.ROUND_HALF_EVEN));


				}//fin del if

		}//fin del for
	}
	@Override
	public String toString(){
		return "Fecha: "+fecha
				+", Id orden: "+idFactura
				+", tipo factura: "+tipoFactura
				+", Cliente"+cliente.getNombre()
				+", subtotal:"+subTotal
				+", Impuesto:"+totalImpuesto
				+", descuento:"+totalDescuento
				+", total:"+total;
	}


	public Integer getIdFactura() {
		return idFactura;
	}

	public void setIdFactura(Integer idFactura) {
		this.idFactura = idFactura;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public BigDecimal getSubTotalExcento() {
		return subTotalExcento;
	}

	public void setSubTotalExcento(BigDecimal subTotalExcento) {
		this.subTotalExcento = subTotalExcento;
	}

	public BigDecimal getSubTotal15() {
		return subTotal15;
	}

	public void setSubTotal15(BigDecimal subTotal15) {
		this.subTotal15 = subTotal15;
	}

	public BigDecimal getSubTotal18() {
		return subTotal18;
	}

	public void setSubTotal18(BigDecimal subTotal18) {
		this.subTotal18 = subTotal18;
	}

	public BigDecimal getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(BigDecimal subTotal) {
		this.subTotal = subTotal;
	}

	public BigDecimal getTotalImpuesto() {
		return totalImpuesto;
	}

	public void setTotalImpuesto(BigDecimal totalImpuesto) {
		this.totalImpuesto = totalImpuesto;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public Integer getClienteId() {
		return clienteId;
	}

	public void setClienteId(Integer clienteId) {
		this.clienteId = clienteId;
	}

	public Integer getCodigo() {
		return codigo;
	}

	public void setCodigo(Integer codigo) {
		this.codigo = codigo;
	}

	public Integer getEstado() {
		return estado;
	}

	public void setEstado(Integer estado) {
		this.estado = estado;
	}

	public BigDecimal getIsvOtros() {
		return isvOtros;
	}

	public void setIsvOtros(BigDecimal isvOtros) {
		this.isvOtros = isvOtros;
	}

	public BigDecimal getTotalImpuesto18() {
		return totalImpuesto18;
	}

	public void setTotalImpuesto18(BigDecimal totalImpuesto18) {
		this.totalImpuesto18 = totalImpuesto18;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public BigDecimal getPago() {
		return pago;
	}

	public void setPago(BigDecimal pago) {
		this.pago = pago;
	}

	public BigDecimal getTotalDescuento() {
		return totalDescuento;
	}

	public void setTotalDescuento(BigDecimal totalDescuento) {
		this.totalDescuento = totalDescuento;
	}

	public Integer getTipoFactura() {
		return tipoFactura;
	}

	public void setTipoFactura(Integer tipoFactura) {
		this.tipoFactura = tipoFactura;
	}

	public Integer getCodigoCaja() {
		return codigoCaja;
	}

	public void setCodigoCaja(Integer codigoCaja) {
		this.codigoCaja = codigoCaja;
	}

	public Integer getVendedorCod() {
		return vendedorCod;
	}

	public void setVendedorCod(Integer vendedorCod) {
		this.vendedorCod = vendedorCod;
	}

	public BigDecimal getTotalOtrosImpuesto() {
		return totalOtrosImpuesto;
	}

	public void setTotalOtrosImpuesto(BigDecimal totalOtrosImpuesto) {
		this.totalOtrosImpuesto = totalOtrosImpuesto;
	}

	public String getFechaVencimento() {
		return fechaVencimento;
	}

	public void setFechaVencimento(String fechaVencimento) {
		this.fechaVencimento = fechaVencimento;
	}

	public int getAgregadoAkardex() {
		return agregadoAkardex;
	}

	public void setAgregadoAkardex(int agregadoAkardex) {
		this.agregadoAkardex = agregadoAkardex;
	}

	public int getTipoPago() {
		return tipoPago;
	}

	public void setTipoPago(int tipoPago) {
		this.tipoPago = tipoPago;
	}

	public BigDecimal getCambio() {
		return cambio;
	}

	public void setCambio(BigDecimal cambio) {
		this.cambio = cambio;
	}

	public BigDecimal getCobroEfectivo() {
		return cobroEfectivo;
	}

	public void setCobroEfectivo(BigDecimal cobroEfectivo) {
		this.cobroEfectivo = cobroEfectivo;
	}

	public BigDecimal getCobroTarjeta() {
		return cobroTarjeta;
	}

	public void setCobroTarjeta(BigDecimal cobroTarjeta) {
		this.cobroTarjeta = cobroTarjeta;
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}

	public boolean isDeseaPagar() {
		return deseaPagar;
	}

	public void setDeseaPagar(boolean deseaPagar) {
		this.deseaPagar = deseaPagar;
	}

	public int getEstadoPago() {
		return estadoPago;
	}

	public void setEstadoPago(int estadoPago) {
		this.estadoPago = estadoPago;
	}

	public int getCodigo2() {
		return codigo2;
	}

	public void setCodigo2(int codigo2) {
		this.codigo2 = codigo2;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Empleado getVendedor() {
		return vendedor;
	}

	public void setVendedor(Empleado vendedor) {
		this.vendedor = vendedor;
	}
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
@Entity
@Table(name = "cliente")
public class Cliente {

	@Id
	@Column(name = "codigo_cliente")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(name = "nombre_cliente")
	private String nombre="NA";

	@Column(name = "direccion")
	private String direccion="NA";

	@Column(name = "telefono")
	private String telefono="NA";

	@Column(name = "movil")
	private String celular="NA";

	@Column(name = "rtn")
	private String rtn="CF";

	@Column(name = "limite_credito")
	private BigDecimal limiteCredito=new BigDecimal(0.0);

	@Transient
	private BigDecimal saldoCuenta=new BigDecimal(0.0);

	@Column(name = "tipo_cliente")
	private Integer tipoCliente=0;

	@Transient
	@ManyToOne
	@JoinColumn(name = "id_vendedor", insertable = false, updatable = false)
	private Empleado vendedor;

	@Column(name = "id_vendedor")
	private int idVendedor=-1;

	
	public void setLimiteCredito(BigDecimal t){
		limiteCredito=limiteCredito.add(t);
	}
	
	public void resetTotales(){
		
		limiteCredito=BigDecimal.ZERO;
		saldoCuenta=BigDecimal.ZERO;
	}
	

	@Override
	public String toString(){
		return "Id:"+id+", rtn:"+rtn+", nombre:"+nombre+", direccion:"+direccion+", telefono:"+telefono+", celular:"+celular;
	}
	public Empleado getVendedor() {
		return vendedor;
	}
	public void setVendedor(Empleado v) {
		this.idVendedor=v.getCodigo();
		this.vendedor = v;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getCelular() {
		return celular;
	}

	public void setCelular(String celular) {
		this.celular = celular;
	}

	public String getRtn() {
		return rtn;
	}

	public void setRtn(String rtn) {
		this.rtn = rtn;
	}

	public BigDecimal getLimiteCredito() {
		return limiteCredito;
	}

	public BigDecimal getSaldoCuenta() {
		return saldoCuenta;
	}

	public void setSaldoCuenta(BigDecimal saldoCuenta) {
		this.saldoCuenta = saldoCuenta;
	}

	public Integer getTipoCliente() {
		return tipoCliente;
	}

	public void setTipoCliente(Integer tipoCliente) {
		this.tipoCliente = tipoCliente;
	}

	public int getIdVendedor() {
		return idVendedor;
	}

	public void setIdVendedor(int idVendedor) {
		this.idVendedor = idVendedor;
	}
}

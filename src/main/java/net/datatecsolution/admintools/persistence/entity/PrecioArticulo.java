package net.datatecsolution.admintools.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Entity
@Table(name = "precios_articulos")
public class PrecioArticulo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "codigo_articulo", insertable = false, updatable = false)
	private Articulo art;

	@ManyToOne
	@JoinColumn(name = "codigo_precio", insertable = false, updatable = false)
	private Precio pre;

	@Column(name = "precio_articulo")
	private BigDecimal precioArticulo;

	@Column(name = "codigo_articulo")
	private Integer articuloId;

	@Column(name = "codigo_precio")
	private Integer precioId;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Articulo getArt() {
		return art;
	}

	public void setArt(Articulo art) {
		this.art = art;
	}

	public Precio getPre() {
		return pre;
	}

	public void setPre(Precio pre) {
		this.pre = pre;
	}

	public BigDecimal getPrecioArticulo() {
		return precioArticulo;
	}

	public void setPrecioArticulo(BigDecimal precioArticulo) {
		this.precioArticulo = precioArticulo;
	}

	public Integer getArticuloId() {
		return articuloId;
	}

	public void setArticuloId(Integer articuloId) {
		this.articuloId = articuloId;
	}

	public Integer getPrecioId() {
		return precioId;
	}

	public void setPrecioId(Integer precioId) {
		this.precioId = precioId;
	}
}

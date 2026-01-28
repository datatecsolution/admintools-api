package net.datatecsolution.admintools.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "precios")
public class Precio {
    @Id
    @Column(name = "codigo_precio")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoPrecio;
    @Column(name = "descripcion")
    private String descripcion;


   @OneToMany(mappedBy = "pre")
   @JsonIgnore
   private List<PrecioArticulo> precioArticulos;

   @OneToMany(mappedBy = "prec")
   @JsonIgnore
   private List<UsuarioPrecio> usuarioPrecios;


    public List<UsuarioPrecio> getUsuarioPrecios() {
        return usuarioPrecios;
    }

    public void setUsuarioPrecios(List<UsuarioPrecio> usuarioPrecios) {
        this.usuarioPrecios = usuarioPrecios;
    }

    public Integer getCodigoPrecio() {
        return codigoPrecio;
    }

    public void setCodigoPrecio(Integer codigoPrecio) {
        this.codigoPrecio = codigoPrecio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<PrecioArticulo> getPrecioArticulos() {
        return precioArticulos;
    }

    public void setPrecioArticulos(List<PrecioArticulo> precioArticulos) {
        this.precioArticulos = precioArticulos;
    }
}

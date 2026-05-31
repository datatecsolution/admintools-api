package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "impuesto")
public class Impuesto {
    @Id
    @Column(name = "codigo_impuesto")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Sprint 4.5+ fix: la columna real es `descripcion_impuesto`. Antes no
    // estaba mapeada; el frontend la necesita para el dropdown de impuestos
    // del modal de producto.
    @Column(name = "descripcion_impuesto")
    private String descripcion;

    private String porcentaje;

    @OneToMany(mappedBy = "imp")
    private List<Articulo> articulos;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(String porcentaje) {
        this.porcentaje = porcentaje;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}

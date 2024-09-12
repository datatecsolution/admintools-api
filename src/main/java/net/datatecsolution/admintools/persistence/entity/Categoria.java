package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "marcas")
public class Categoria {
    @Id
    @Column(name = "codigo_marca")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "observacion")
    private String observacion;

    @OneToMany(mappedBy = "categoria")
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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}

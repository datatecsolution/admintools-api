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

    /** ¿La categoría se muestra/selecciona en el POS táctil? (mostrar_pos, V29). */
    @Column(name = "mostrar_pos")
    private Boolean mostrarPos = false;

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

    public Boolean getMostrarPos() {
        return mostrarPos;
    }

    public void setMostrarPos(Boolean mostrarPos) {
        this.mostrarPos = mostrarPos;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}

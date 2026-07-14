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

    /**
     * Categoría padre (US-081, V38): self-FK a marcas.codigo_marca.
     * NULL = raíz. Se mapea el id plano (no ManyToOne): el árbol se arma en
     * CategoryService con una sola pasada en memoria.
     */
    @Column(name = "parent_id")
    private Integer parentId;

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

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}

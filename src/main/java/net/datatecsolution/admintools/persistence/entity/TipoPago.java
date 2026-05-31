package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * US-032 — Metodo de pago aceptado (BD comun, tabla tipo_pago). Catalogo
 * editable desde el panel admin; lo referencian las facturas (tipo_pago).
 */
@Entity
@Table(name = "tipo_pago")
public class TipoPago {

    @Id
    @Column(name = "codigo_tipo_pago")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "descripcion")
    private String descripcion;

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
}

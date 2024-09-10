package net.datatecsolution.admintools.persistence.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class PreciosArticulosKey implements Serializable {


    @Column(name = "codigo_articulo",insertable=false, updatable=false)
    private Integer articuloId;
    @Column(name = "codigo_precio",insertable=false, updatable=false)
    private Integer precioId;

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

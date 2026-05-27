package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad para la tabla 'bodega' (admin_tools). Representa las ubicaciones
 * fisicas/logicas donde el inventario se mantiene (Tienda Principal, Bodega 1,
 * Perdidas, etc.). Cada bodega tiene una entrada espejo en {@code departamento}.
 *
 * Introducida en INV-1 para enriquecer ExistenciaResponse con el nombre de
 * la bodega. INV-3 agregara CRUD completo.
 */
@Entity
@Table(name = "bodega")
public class Bodega {

    @Id
    @Column(name = "codigo_bodega")
    private Integer codigoBodega;   // INV-3 lo asigna a mano para sincronizar con Departamento

    @Column(name = "descripcion_bodega")
    private String descripcionBodega;

    public Bodega() {
    }

    public Bodega(Integer codigoBodega, String descripcionBodega) {
        this.codigoBodega = codigoBodega;
        this.descripcionBodega = descripcionBodega;
    }

    public Integer getCodigoBodega() {
        return codigoBodega;
    }

    public void setCodigoBodega(Integer codigoBodega) {
        this.codigoBodega = codigoBodega;
    }

    public String getDescripcionBodega() {
        return descripcionBodega;
    }

    public void setDescripcionBodega(String descripcionBodega) {
        this.descripcionBodega = descripcionBodega;
    }
}

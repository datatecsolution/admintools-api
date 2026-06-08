package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * US-034 — Configuracion global de facturacion (BD comun, tabla single-row).
 * Solo se necesita {@code dia_vencimiento_factura} para calcular el
 * vencimiento de las ventas a credito (igual que el Swing).
 *
 * La tabla {@code config_app} no tiene PK; usamos {@code dia_vencimiento_factura}
 * como @Id solo para satisfacer a JPA en una entidad de lectura. ddl-auto=validate
 * unicamente exige que las columnas mapeadas existan.
 */
@Entity
@Table(name = "config_app")
public class ConfigApp {

    @Id
    @Column(name = "dia_vencimiento_factura")
    private Integer diaVencimientoFactura;

    @Column(name = "interes_para_facturas_venc")
    private Integer interesParaFacturasVenc;

    public Integer getDiaVencimientoFactura() {
        return diaVencimientoFactura;
    }

    public void setDiaVencimientoFactura(Integer diaVencimientoFactura) {
        this.diaVencimientoFactura = diaVencimientoFactura;
    }

    public Integer getInteresParaFacturasVenc() {
        return interesParaFacturasVenc;
    }

    public void setInteresParaFacturasVenc(Integer interesParaFacturasVenc) {
        this.interesParaFacturasVenc = interesParaFacturasVenc;
    }
}

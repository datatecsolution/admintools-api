package net.datatecsolution.admintools.persistence.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Checkout POS — rango de facturación CAI de la caja (BD tenant, tabla
 * datos_factura). El rango activo (mayor codigo_rango) provee el CAI que se
 * enlaza a cada factura vía encabezado_factura.cod_rango, igual que el Swing
 * (DatosFacturacionDao.getIdCaiAct). Solo lectura aquí.
 */
@Entity
@Table(name = "datos_factura")
public class DatosFactura {

    @Id
    @Column(name = "codigo_rango")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoRango;

    @Column(name = "CAI")
    private String cai;

    public Integer getCodigoRango() {
        return codigoRango;
    }

    public void setCodigoRango(Integer codigoRango) {
        this.codigoRango = codigoRango;
    }

    public String getCai() {
        return cai;
    }

    public void setCai(String cai) {
        this.cai = cai;
    }
}

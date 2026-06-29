package net.datatecsolution.admintools.persistence.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Checkout POS — rango de facturación CAI de la caja (BD tenant, tabla
 * datos_factura). El rango activo (mayor codigo_rango) provee el CAI que se
 * enlaza a cada factura vía encabezado_factura.cod_rango, igual que el Swing
 * (DatosFacturacionDao.getIdCaiAct). Solo lectura aquí.
 *
 * US-040: además del CAI, expone el resto del rango autorizado SAR
 * (factura_inicial/final, prefijo codigo_tipo_facturacion y fecha límite de
 * emisión) para imprimir el ticket fiscal — los administra el Swing
 * (DatosFacturacion), aquí no se escriben.
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

    /** Nº inicial autorizado del rango (varchar en BD; numérico salvo 'NA'). */
    @Column(name = "factura_inicial")
    private String facturaInicial;

    /** Nº final autorizado del rango (varchar en BD; numérico salvo 'NA'). */
    @Column(name = "factura_final")
    private String facturaFinal;

    /** Prefijo SAR del número (ej. "01" o "000-001-01"). */
    @Column(name = "codigo_tipo_facturacion")
    private String codigoTipoFacturacion;

    @Column(name = "cantida_solicitada")
    private Integer cantidaSolicitada;

    @Column(name = "fecha_limite_emision")
    private LocalDate fechaLimiteEmision;

    /** Observación del rango = sucursal que imprime el ticket (obsRango del Swing). */
    @Column(name = "observacion")
    private String observacion;

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

    public String getFacturaInicial() {
        return facturaInicial;
    }

    public void setFacturaInicial(String facturaInicial) {
        this.facturaInicial = facturaInicial;
    }

    public String getFacturaFinal() {
        return facturaFinal;
    }

    public void setFacturaFinal(String facturaFinal) {
        this.facturaFinal = facturaFinal;
    }

    public String getCodigoTipoFacturacion() {
        return codigoTipoFacturacion;
    }

    public void setCodigoTipoFacturacion(String codigoTipoFacturacion) {
        this.codigoTipoFacturacion = codigoTipoFacturacion;
    }

    public Integer getCantidaSolicitada() {
        return cantidaSolicitada;
    }

    public void setCantidaSolicitada(Integer cantidaSolicitada) {
        this.cantidaSolicitada = cantidaSolicitada;
    }

    public LocalDate getFechaLimiteEmision() {
        return fechaLimiteEmision;
    }

    public void setFechaLimiteEmision(LocalDate fechaLimiteEmision) {
        this.fechaLimiteEmision = fechaLimiteEmision;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}

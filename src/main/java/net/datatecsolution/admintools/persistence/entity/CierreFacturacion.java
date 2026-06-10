package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rango de facturas procesado por caja dentro de un cierre (turno). Mirror
 * de {@code CierreFacturacion} del Swing: en la apertura se crea una fila
 * por caja del usuario con {@code factura_inicial} = ultima final + 1; al
 * cerrar se completa {@code factura_final} con la ultima factura emitida.
 */
@Entity
@Table(name = "cierre_facturacion")
public class CierreFacturacion {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_cierre")
    private Integer codigoCierre = 0;

    @Column(name = "codigo_caja")
    private Integer codigoCaja = 0;

    @Column(name = "usuario")
    private String usuario = "system";

    @Column(name = "factura_inicial")
    private Integer facturaInicial = -1;

    @Column(name = "factura_final")
    private Integer facturaFinal = -1;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCodigoCierre() { return codigoCierre; }
    public void setCodigoCierre(Integer codigoCierre) { this.codigoCierre = codigoCierre; }
    public Integer getCodigoCaja() { return codigoCaja; }
    public void setCodigoCaja(Integer codigoCaja) { this.codigoCaja = codigoCaja; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Integer getFacturaInicial() { return facturaInicial; }
    public void setFacturaInicial(Integer facturaInicial) { this.facturaInicial = facturaInicial; }
    public Integer getFacturaFinal() { return facturaFinal; }
    public void setFacturaFinal(Integer facturaFinal) { this.facturaFinal = facturaFinal; }
}

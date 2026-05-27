package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;

/**
 * POJO de dominio que representa el saldo de un articulo en una bodega
 * en un momento dado. Mapeado desde {@code ExistenciaArticuloBodega} por
 * {@code ExistenciaMapper}. Convertido a {@code ExistenciaResponse} para
 * exponerlo via API.
 */
public class Existencia {

    private Integer codigoArticulo;
    private Integer codigoBodega;
    private String descripcionBodega;
    private BigDecimal cantidad;

    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }

    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }

    public String getDescripcionBodega() { return descripcionBodega; }
    public void setDescripcionBodega(String descripcionBodega) { this.descripcionBodega = descripcionBodega; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
}

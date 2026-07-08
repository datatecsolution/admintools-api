package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * US-035 — Configuracion de kardex por (articulo, bodega) en la BD comun:
 * umbrales de stock minimo/maximo y metodo de valoracion. El saldo de
 * cantidad vive en {@code existencia_articulo_bodega}; el costo promedio se
 * obtiene con la funcion {@code f_precio_saldo_kardex(codigo_kardex)}.
 */
@Entity
@Table(name = "articulo_kardex")
public class ArticuloKardex {

    @Id
    @Column(name = "codigo_kardex")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoKardex;

    @Column(name = "codigo_articulo")
    private Integer codigoArticulo;

    @Column(name = "codigo_bodega")
    private Integer codigoBodega;

    // US-070 (V34 common): kardex migrado a DECIMAL(15,2). BigDecimal mapea a
    // DECIMAL por defecto → validate coincide con el nuevo tipo.
    @Column(name = "cantidad_minima")
    private BigDecimal cantidadMinima;

    @Column(name = "cantidad_maxima")
    private BigDecimal cantidadMaxima;

    @Column(name = "metodo")
    private String metodo;

    public Integer getCodigoKardex() {
        return codigoKardex;
    }

    public void setCodigoKardex(Integer codigoKardex) {
        this.codigoKardex = codigoKardex;
    }

    public Integer getCodigoArticulo() {
        return codigoArticulo;
    }

    public void setCodigoArticulo(Integer codigoArticulo) {
        this.codigoArticulo = codigoArticulo;
    }

    public Integer getCodigoBodega() {
        return codigoBodega;
    }

    public void setCodigoBodega(Integer codigoBodega) {
        this.codigoBodega = codigoBodega;
    }

    public BigDecimal getCantidadMinima() {
        return cantidadMinima;
    }

    public void setCantidadMinima(BigDecimal cantidadMinima) {
        this.cantidadMinima = cantidadMinima;
    }

    public BigDecimal getCantidadMaxima() {
        return cantidadMaxima;
    }

    public void setCantidadMaxima(BigDecimal cantidadMaxima) {
        this.cantidadMaxima = cantidadMaxima;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }
}

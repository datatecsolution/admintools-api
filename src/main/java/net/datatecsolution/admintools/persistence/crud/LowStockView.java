package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;

/**
 * US-035 — Proyeccion de alerta de stock minimo: existencia por debajo (o
 * igual) del umbral {@code cantidad_minima} del kardex.
 */
public interface LowStockView {
    Integer getCodigoArticulo();
    String getArticulo();
    Integer getCodigoBodega();
    BigDecimal getCantidad();
    BigDecimal getCantidadMinima();
    BigDecimal getFaltante();
}

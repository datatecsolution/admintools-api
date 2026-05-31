package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;

/**
 * US-035 — Proyeccion de valoracion de inventario por (articulo, bodega):
 * cantidad x costo promedio (f_precio_saldo_kardex) = valor total.
 */
public interface StockValuationView {
    Integer getCodigoArticulo();
    String getArticulo();
    Integer getCodigoBodega();
    BigDecimal getCantidad();
    BigDecimal getCostoUnitario();
    BigDecimal getValorTotal();
}

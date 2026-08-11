package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;

/**
 * US-141 — existencia de un articulo en UNA bodega, para adjuntarla a la
 * pagina de productos.
 *
 * Es la misma informacion que da /inventory/valuation, pero acotada a los
 * articulos de la pagina que se esta devolviendo. Antes el POS pedia la
 * valoracion COMPLETA (size=1000) para pintar una columna: con los 3.423
 * articulos de un cliente real, 2.423 quedaban fuera del tope y se mostraban
 * como existencia 0 / agotado aunque tuvieran stock.
 *
 * Incluye cantidadMinima para que el nivel (ok/bajo/agotado) se resuelva sin
 * una segunda consulta de low-stock.
 */
public interface ProductStockView {
    Integer getCodigoArticulo();
    BigDecimal getCantidad();
    BigDecimal getCostoUnitario();
    BigDecimal getReservado();
    BigDecimal getDisponible();
    BigDecimal getCantidadMinima();
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-141 — existencia de un producto en la bodega consultada, embebida en
 * la pagina de {@code GET /products}.
 *
 * Va como sub-objeto y no como campos sueltos para dejar claro que TODO el
 * bloque es relativo a una bodega: sin {@code warehouse} en la peticion, el
 * campo viaja en null y el cliente no muestra columna de existencia.
 *
 * @param nivel "agotado" (sin unidades), "bajo" (a nivel o debajo del minimo
 *              del kardex) u "ok". Se calcula en el servidor con la misma
 *              regla de /inventory/low-stock para que el semaforo del POS no
 *              dependa de una segunda consulta.
 */
public record ProductStock(
        BigDecimal cantidad,
        BigDecimal reservado,
        BigDecimal disponible,
        BigDecimal costoUnitario,
        BigDecimal cantidadMinima,
        String nivel
) {
    public static final String AGOTADO = "agotado";
    public static final String BAJO = "bajo";
    public static final String OK = "ok";

    /** Producto sin fila de existencia en la bodega: existe, pero no hay nada. */
    public static ProductStock vacio() {
        return new ProductStock(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, AGOTADO);
    }

    public static ProductStock de(BigDecimal cantidad, BigDecimal reservado, BigDecimal disponible,
                                  BigDecimal costoUnitario, BigDecimal cantidadMinima) {
        BigDecimal cant = cantidad == null ? BigDecimal.ZERO : cantidad;
        BigDecimal min = cantidadMinima == null ? BigDecimal.ZERO : cantidadMinima;
        String nivel = cant.signum() <= 0 ? AGOTADO
                : (cant.compareTo(min) <= 0 ? BAJO : OK);
        return new ProductStock(cant,
                reservado == null ? BigDecimal.ZERO : reservado,
                disponible == null ? cant : disponible,
                costoUnitario == null ? BigDecimal.ZERO : costoUnitario,
                min, nivel);
    }
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Línea en conflicto de stock al guardar una orden (US-074): el usuario
 * pidió más de lo disponible (saldo kardex − órdenes pendientes, bodega 1,
 * misma fuente que la columna existencia de articulo_view).
 */
public class StockConflict {

    private final int productId;
    private final String nombre;
    private final BigDecimal pedida;
    private final BigDecimal disponible;

    public StockConflict(int productId, String nombre, BigDecimal pedida, BigDecimal disponible) {
        this.productId = productId;
        this.nombre = nombre;
        this.pedida = pedida;
        this.disponible = disponible;
    }

    public int getProductId() {
        return productId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPedida() {
        return pedida;
    }

    public BigDecimal getDisponible() {
        return disponible;
    }
}

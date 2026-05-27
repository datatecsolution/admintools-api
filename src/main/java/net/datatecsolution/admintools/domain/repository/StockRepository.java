package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Stock;

import java.math.BigDecimal;
import java.util.List;

/**
 * Abstraccion del backend de lectura de stock. INV-1 la implementa contra
 * la tabla {@code existencia_articulo_bodega} (cache transaccional del
 * kardex, mantenida por los SPs de V19/V20).
 *
 * Manana se podria swappear a otra fuente (funcion, vista, materialized
 * view a otra capa) sin tocar controllers ni servicios — todo el contrato
 * de stock entra y sale por aqui.
 */
public interface StockRepository {

    /** Saldo actual del producto en la bodega. Devuelve 0 si no hay registro. */
    BigDecimal getStock(int productCode, int warehouseCode);

    /**
     * Saldo + metadata de la bodega para una (product, warehouse) especifica.
     * Devuelve Stock con warehouseDescription rellena cuando hay registro;
     * si no hay registro devuelve null (el service decide como exponerlo).
     */
    Stock getStockDetail(int productCode, int warehouseCode);

    /** Saldo del producto en TODAS las bodegas donde tiene kardex. */
    List<Stock> getStockByProduct(int productCode);
}

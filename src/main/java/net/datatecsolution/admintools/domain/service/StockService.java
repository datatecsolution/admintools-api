package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Stock;
import net.datatecsolution.admintools.domain.dto.StockResponse;
import net.datatecsolution.admintools.domain.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de lectura de stock (INV-1). Lee detras de
 * {@link StockRepository}, que hoy va contra la tabla materializada
 * {@code existencia_articulo_bodega}.
 *
 * Convierte el POJO de dominio {@link Stock} al DTO {@link StockResponse}.
 */
@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    /**
     * Saldo de un articulo en una bodega especifica.
     * Si la combinacion no tiene registro en la tabla de saldos, devuelve
     * cantidad 0 (semantica: "stock cero, no error"). descripcionBodega
     * solo se rellena cuando si hay registro (con el JOIN a bodega).
     */
    public StockResponse getStock(int productCode, int warehouseCode) {
        Stock s = stockRepository.getStockDetail(productCode, warehouseCode);
        if (s == null) {
            return new StockResponse(productCode, warehouseCode, null, BigDecimal.ZERO);
        }
        return new StockResponse(
                s.getProductCode(), s.getWarehouseCode(),
                s.getWarehouseDescription(), s.getQuantity());
    }

    /** Stock del producto en TODAS las bodegas donde tiene kardex. */
    public List<StockResponse> getStockByProduct(int productCode) {
        return stockRepository.getStockByProduct(productCode).stream()
                .map(s -> new StockResponse(
                        s.getProductCode(),
                        s.getWarehouseCode(),
                        s.getWarehouseDescription(),
                        s.getQuantity()))
                .toList();
    }
}

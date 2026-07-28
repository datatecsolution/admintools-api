package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Stock;
import net.datatecsolution.admintools.domain.dto.StockResponse;
import net.datatecsolution.admintools.domain.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private net.datatecsolution.admintools.persistence.crud.ArticuloKardexCRUD articuloKardexCRUD;

    /**
     * Saldo de un articulo en una bodega especifica.
     * Si la combinacion no tiene registro en la tabla de saldos, devuelve
     * cantidad 0 (semantica: "stock cero, no error"). descripcionBodega
     * solo se rellena cuando si hay registro (con el JOIN a bodega).
     *
     * US-112: quantity sigue siendo el FISICO; reserved = pedidos pendientes
     * de la bodega (v_reservado_por_articulo) y available = quantity − reserved.
     */
    public StockResponse getStock(int productCode, int warehouseCode) {
        BigDecimal reserved = articuloKardexCRUD.findReservado(productCode, warehouseCode)
                .orElse(BigDecimal.ZERO);
        Stock s = stockRepository.getStockDetail(productCode, warehouseCode);
        if (s == null) {
            return new StockResponse(productCode, warehouseCode, null,
                    BigDecimal.ZERO, reserved, BigDecimal.ZERO.subtract(reserved));
        }
        return new StockResponse(
                s.getProductCode(), s.getWarehouseCode(),
                s.getWarehouseDescription(), s.getQuantity(),
                reserved, s.getQuantity().subtract(reserved));
    }

    /** Stock del producto en TODAS las bodegas donde tiene kardex. */
    public List<StockResponse> getStockByProduct(int productCode) {
        // US-112: reservado de todas las bodegas en UNA consulta (no una por fila).
        Map<Integer, BigDecimal> reservadoPorBodega = new HashMap<>();
        for (Object[] fila : articuloKardexCRUD.findReservadoPorBodega(productCode)) {
            reservadoPorBodega.put(((Number) fila[0]).intValue(), new BigDecimal(fila[1].toString()));
        }
        return stockRepository.getStockByProduct(productCode).stream()
                .map(s -> {
                    BigDecimal reserved = reservadoPorBodega.getOrDefault(
                            s.getWarehouseCode(), BigDecimal.ZERO);
                    return new StockResponse(
                            s.getProductCode(),
                            s.getWarehouseCode(),
                            s.getWarehouseDescription(),
                            s.getQuantity(),
                            reserved,
                            s.getQuantity().subtract(reserved));
                })
                .toList();
    }
}

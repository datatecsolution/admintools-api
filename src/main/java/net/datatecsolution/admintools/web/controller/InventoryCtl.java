package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.StockResponse;
import net.datatecsolution.admintools.domain.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de lectura del subsistema de inventario (INV-1).
 *
 * Vive bajo {@code /inventory} (no /products) para no chocar con el legacy
 * {@code ProductCtl}. Lee siempre detras de {@link StockService}, que
 * a su vez consulta la tabla {@code existencia_articulo_bodega} mantenida
 * transaccionalmente por los SPs del kardex (V19/V20).
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory", description = "Lectura de stock por producto y bodega")
public class InventoryCtl {

    private final StockService stockService;

    public InventoryCtl(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock")
    @Operation(summary = "Saldo actual de un producto en una bodega especifica")
    public ResponseEntity<StockResponse> getStock(
            @RequestParam("product") int product,
            @RequestParam(name = "warehouse", defaultValue = "1") int warehouse) {
        return ResponseEntity.ok(stockService.getStock(product, warehouse));
    }

    @GetMapping("/product/{id}/stock")
    @Operation(summary = "Saldo de un producto en todas las bodegas donde tiene kardex")
    public ResponseEntity<List<StockResponse>> getStockByProduct(
            @PathVariable("id") int productId) {
        return ResponseEntity.ok(stockService.getStockByProduct(productId));
    }
}

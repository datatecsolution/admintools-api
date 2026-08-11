package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.KardexMovementResponse;
import net.datatecsolution.admintools.domain.dto.LowStockResponse;
import net.datatecsolution.admintools.domain.dto.StockResponse;
import net.datatecsolution.admintools.domain.dto.StockValuationResponse;
import net.datatecsolution.admintools.domain.dto.ValuationSummaryResponse;
import net.datatecsolution.admintools.domain.service.InventoryReportService;
import net.datatecsolution.admintools.domain.service.StockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de lectura del subsistema de inventario.
 *
 * Vive bajo {@code /inventory} (no /products) para no chocar con el legacy
 * {@code ProductCtl}. El saldo puntual (INV-1) se lee via {@link StockService}
 * sobre {@code existencia_articulo_bodega} (mantenida por los SPs del kardex,
 * V19/V20). US-035 anade movimientos, valoracion (promedio ponderado) y
 * alertas de stock minimo via {@link InventoryReportService}.
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory", description = "Stock, movimientos, valoracion y alertas de inventario")
public class InventoryCtl {

    private final StockService stockService;
    private final InventoryReportService inventoryReportService;

    public InventoryCtl(StockService stockService, InventoryReportService inventoryReportService) {
        this.stockService = stockService;
        this.inventoryReportService = inventoryReportService;
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

    // ---- US-035: movimientos, valoracion y alertas ----

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Historial de movimientos del kardex de un producto en una bodega")
    public ResponseEntity<Page<KardexMovementResponse>> getMovements(
            @RequestParam("product") int product,
            @RequestParam(name = "warehouse", defaultValue = "1") int warehouse,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(
                inventoryReportService.getMovements(product, warehouse, PageRequest.of(page, size)));
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Valoracion de inventario por producto/bodega a costo promedio")
    public ResponseEntity<Page<StockValuationResponse>> getValuation(
            @RequestParam(name = "warehouse", required = false) Integer warehouse,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(
                inventoryReportService.getValuation(warehouse, name, PageRequest.of(page, size)));
    }

    @GetMapping("/valuation/summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Valor total del inventario (opcionalmente por bodega)")
    public ResponseEntity<ValuationSummaryResponse> getValuationSummary(
            @RequestParam(name = "warehouse", required = false) Integer warehouse) {
        return ResponseEntity.ok(inventoryReportService.getValuationTotal(warehouse));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Alertas de stock minimo (existencia <= cantidad_minima)")
    public ResponseEntity<Page<LowStockResponse>> getLowStock(
            @RequestParam(name = "warehouse", required = false) Integer warehouse,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(
                inventoryReportService.getLowStock(warehouse, name, PageRequest.of(page, size)));
    }
}

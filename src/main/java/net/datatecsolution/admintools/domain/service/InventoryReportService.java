package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.KardexMovementResponse;
import net.datatecsolution.admintools.domain.dto.LowStockResponse;
import net.datatecsolution.admintools.domain.dto.StockValuationResponse;
import net.datatecsolution.admintools.domain.dto.ValuationSummaryResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloKardexCRUD;
import net.datatecsolution.admintools.persistence.entity.ArticuloKardex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * US-035 — Reportes de inventario: historial de movimientos del kardex,
 * valoracion por costo promedio (reusa la funcion MySQL
 * {@code f_precio_saldo_kardex}) y alertas de stock minimo. Lectura sobre la
 * BD comun. La lectura de saldo puntual sigue en {@link StockService} (INV-1).
 */
@Service
public class InventoryReportService {

    private final ArticuloKardexCRUD articuloKardexCRUD;

    public InventoryReportService(ArticuloKardexCRUD articuloKardexCRUD) {
        this.articuloKardexCRUD = articuloKardexCRUD;
    }

    /** Historial de movimientos de un (articulo, bodega). 404 si no tiene kardex. */
    public Page<KardexMovementResponse> getMovements(int product, int warehouse, Pageable pageable) {
        ArticuloKardex kardex = articuloKardexCRUD
                .findByCodigoArticuloAndCodigoBodega(product, warehouse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe kardex para el articulo " + product + " en la bodega " + warehouse));
        return articuloKardexCRUD.findMovements(kardex.getCodigoKardex(), pageable)
                .map(v -> new KardexMovementResponse(
                        v.getCodigoMovimiento(), v.getFecha(), v.getTipoMovimiento(),
                        v.getTipoMovimientoDesc(), v.getDescripcion(), v.getDocumento(),
                        v.getCantidad(), v.getPrecioUnidad(), v.getTotal()));
    }

    /** Valoracion por (articulo, bodega); {@code warehouse} null = todas. */
    public Page<StockValuationResponse> getValuation(Integer warehouse, Pageable pageable) {
        return articuloKardexCRUD.findValuation(warehouse, pageable)
                .map(v -> new StockValuationResponse(
                        v.getCodigoArticulo(), v.getArticulo(), v.getCodigoBodega(),
                        v.getCantidad(), v.getCostoUnitario(), v.getValorTotal(),
                        v.getReservado(), v.getDisponible()));
    }

    /** Valor total del inventario; {@code warehouse} null = todas las bodegas. */
    public ValuationSummaryResponse getValuationTotal(Integer warehouse) {
        BigDecimal total = articuloKardexCRUD.sumValuation(warehouse);
        return new ValuationSummaryResponse(warehouse, total == null ? BigDecimal.ZERO : total);
    }

    /** Alertas de stock minimo; {@code warehouse} null = todas las bodegas. */
    public Page<LowStockResponse> getLowStock(Integer warehouse, Pageable pageable) {
        return articuloKardexCRUD.findLowStock(warehouse, pageable)
                .map(v -> new LowStockResponse(
                        v.getCodigoArticulo(), v.getArticulo(), v.getCodigoBodega(),
                        v.getCantidad(), v.getCantidadMinima(), v.getFaltante()));
    }
}

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.KardexMovementResponse;
import net.datatecsolution.admintools.domain.dto.LowStockResponse;
import net.datatecsolution.admintools.domain.dto.StockValuationResponse;
import net.datatecsolution.admintools.domain.dto.ValuationSummaryResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloKardexCRUD;
import net.datatecsolution.admintools.persistence.crud.KardexMovementView;
import net.datatecsolution.admintools.persistence.crud.LowStockView;
import net.datatecsolution.admintools.persistence.crud.StockValuationView;
import net.datatecsolution.admintools.persistence.entity.ArticuloKardex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * US-035 — Unit tests del servicio de reportes de inventario.
 */
@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {

    @Mock private ArticuloKardexCRUD articuloKardexCRUD;

    private InventoryReportService service() {
        return new InventoryReportService(articuloKardexCRUD);
    }

    private final Pageable page = PageRequest.of(0, 20);

    @Test
    void getMovements_sinKardex_lanza404() {
        when(articuloKardexCRUD.findByCodigoArticuloAndCodigoBodega(5, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getMovements(5, 1, page))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getMovements_conKardex_mapeaProyeccion() {
        ArticuloKardex k = new ArticuloKardex();
        k.setCodigoKardex(42);
        when(articuloKardexCRUD.findByCodigoArticuloAndCodigoBodega(5, 1)).thenReturn(Optional.of(k));

        KardexMovementView v = mock(KardexMovementView.class);
        when(v.getCodigoMovimiento()).thenReturn(7);
        when(v.getTipoMovimientoDesc()).thenReturn("Salida por venta");
        when(v.getCantidad()).thenReturn(new BigDecimal("3.00"));
        when(v.getPrecioUnidad()).thenReturn(new BigDecimal("10.00"));
        when(v.getTotal()).thenReturn(new BigDecimal("30.00"));
        when(articuloKardexCRUD.findMovements(eq(42), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        Page<KardexMovementResponse> res = service().getMovements(5, 1, page);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).codigoMovimiento()).isEqualTo(7);
        assertThat(res.getContent().get(0).tipoMovimientoDesc()).isEqualTo("Salida por venta");
        assertThat(res.getContent().get(0).total()).isEqualByComparingTo("30.00");
    }

    @Test
    void getValuation_mapeaProyeccion() {
        StockValuationView v = mock(StockValuationView.class);
        when(v.getCodigoArticulo()).thenReturn(5);
        when(v.getArticulo()).thenReturn("Coca Cola");
        when(v.getCantidad()).thenReturn(new BigDecimal("10.00"));
        when(v.getCostoUnitario()).thenReturn(new BigDecimal("12.50"));
        when(v.getValorTotal()).thenReturn(new BigDecimal("125.00"));
        when(articuloKardexCRUD.findValuation(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        Page<StockValuationResponse> res = service().getValuation(1, page);

        assertThat(res.getContent().get(0).articulo()).isEqualTo("Coca Cola");
        assertThat(res.getContent().get(0).valorTotal()).isEqualByComparingTo("125.00");
    }

    @Test
    void getValuationTotal_conBodega_devuelveTotal() {
        when(articuloKardexCRUD.sumValuation(2)).thenReturn(new BigDecimal("999.99"));

        ValuationSummaryResponse res = service().getValuationTotal(2);

        assertThat(res.warehouse()).isEqualTo(2);
        assertThat(res.valorTotal()).isEqualByComparingTo("999.99");
    }

    @Test
    void getValuationTotal_nullDelDb_devuelveCero() {
        when(articuloKardexCRUD.sumValuation(null)).thenReturn(null);

        ValuationSummaryResponse res = service().getValuationTotal(null);

        assertThat(res.warehouse()).isNull();
        assertThat(res.valorTotal()).isEqualByComparingTo("0");
    }

    @Test
    void getLowStock_mapeaProyeccion() {
        LowStockView v = mock(LowStockView.class);
        when(v.getCodigoArticulo()).thenReturn(5);
        when(v.getArticulo()).thenReturn("Coca Cola");
        when(v.getCantidad()).thenReturn(new BigDecimal("2.00"));
        when(v.getCantidadMinima()).thenReturn(new BigDecimal("20.00"));
        when(v.getFaltante()).thenReturn(new BigDecimal("18.00"));
        when(articuloKardexCRUD.findLowStock(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        Page<LowStockResponse> res = service().getLowStock(null, page);

        assertThat(res.getContent().get(0).faltante()).isEqualByComparingTo("18.00");
        assertThat(res.getContent().get(0).cantidadMinima()).isEqualByComparingTo("20.00");
    }
}

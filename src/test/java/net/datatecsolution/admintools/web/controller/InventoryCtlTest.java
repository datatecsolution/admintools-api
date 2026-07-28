package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.LowStockResponse;
import net.datatecsolution.admintools.domain.dto.StockValuationResponse;
import net.datatecsolution.admintools.domain.dto.ValuationSummaryResponse;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.InventoryReportService;
import net.datatecsolution.admintools.domain.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-035 — Slice de los endpoints de reportes de inventario.
 */
@WebMvcTest(controllers = InventoryCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryCtlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @MockBean
    private InventoryReportService inventoryReportService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean(name = "commonDataSource")
    private DataSource commonDataSource;

    @Test
    @WithMockUser(roles = "INVENTORY")
    void valuation_retorna200() throws Exception {
        Page<StockValuationResponse> page = new PageImpl<>(List.of(
                new StockValuationResponse(5, "Coca Cola", 1,
                        new BigDecimal("10.00"), new BigDecimal("12.50"), new BigDecimal("125.00"),
                        new BigDecimal("2.00"), new BigDecimal("8.00"))));
        when(inventoryReportService.getValuation(eq(1), any())).thenReturn(page);

        mockMvc.perform(get("/inventory/valuation").param("warehouse", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articulo").value("Coca Cola"))
                .andExpect(jsonPath("$.content[0].valorTotal").value(125.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void valuationSummary_retorna200() throws Exception {
        when(inventoryReportService.getValuationTotal(null))
                .thenReturn(new ValuationSummaryResponse(null, new BigDecimal("4321.00")));

        mockMvc.perform(get("/inventory/valuation/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(4321.00));
    }

    @Test
    @WithMockUser(roles = "INVENTORY")
    void lowStock_retorna200() throws Exception {
        Page<LowStockResponse> page = new PageImpl<>(List.of(
                new LowStockResponse(5, "Coca Cola", 1,
                        new BigDecimal("2.00"), new BigDecimal("20.00"), new BigDecimal("18.00"))));
        when(inventoryReportService.getLowStock(eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].faltante").value(18.00));
    }
}

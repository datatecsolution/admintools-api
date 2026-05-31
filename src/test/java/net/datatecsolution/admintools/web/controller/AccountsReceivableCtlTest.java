package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.AbonoRequest;
import net.datatecsolution.admintools.domain.dto.BalanceResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptResponse;
import net.datatecsolution.admintools.domain.service.AccountsReceivableService;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-033 — Slice de {@link AccountsReceivableCtl}: status, shape de DTOs y
 * enforcement de @PreAuthorize por rol (con @WithMockUser).
 */
@WebMvcTest(controllers = AccountsReceivableCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountsReceivableCtlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountsReceivableService service;

    // Beans requeridos por la SecurityConfig que carga el slice
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // Requerido por TenantInterceptor (HandlerInterceptor que el slice levanta).
    // Su preHandle tolera getConnection() nulo (try/catch), asi que el mock basta.
    @MockBean(name = "commonDataSource")
    private DataSource commonDataSource;

    @Test
    @WithMockUser(roles = "CASHIER")
    void getBalance_retorna200ConSaldo() throws Exception {
        when(service.getBalance(7)).thenReturn(new BalanceResponse(
                7, "Cliente 7", new BigDecimal("500.00"), new BigDecimal("120.00"), new BigDecimal("380.00")));

        mockMvc.perform(get("/accounts-receivable/7/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.saldo").value(120.00))
                .andExpect(jsonPath("$.disponible").value(380.00));
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void applyPayment_valido_retorna201() throws Exception {
        when(service.applyPayment(eq(7), any(AbonoRequest.class), any()))
                .thenReturn(new ReceiptResponse(
                        5, LocalDateTime.now(), 7, new BigDecimal("30.00"), "Pago parcial",
                        "TR-1", "ronal", new BigDecimal("100.00"), new BigDecimal("70.00")));

        mockMvc.perform(post("/accounts-receivable/7/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AbonoRequest(new BigDecimal("30"), "Pago parcial", "TR-1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noRecibo").value(5))
                .andExpect(jsonPath("$.saldo").value(70.00));
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void applyPayment_montoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/accounts-receivable/7/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AbonoRequest(new BigDecimal("-5"), "x", "y"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delinquent_conRolAdmin_retorna200() throws Exception {
        Page<?> empty = new PageImpl<>(List.of());
        when(service.listDelinquent(eq(30), any())).thenReturn((Page) empty);

        mockMvc.perform(get("/accounts-receivable/delinquent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

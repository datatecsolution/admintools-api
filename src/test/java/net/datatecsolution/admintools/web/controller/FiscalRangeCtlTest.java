package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.FiscalRangeRequest;
import net.datatecsolution.admintools.domain.dto.FiscalRangeResponse;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.FiscalRangeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-101 — slice del controller de rangos fiscales (datos_factura por caja).
 */
@WebMvcTest(controllers = FiscalRangeCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class FiscalRangeCtlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FiscalRangeService service;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean(name = "commonDataSource") private DataSource commonDataSource;

    private FiscalRangeResponse range() {
        return new FiscalRangeResponse(7, "CAI-ABC-123", 1001, 2000, "000-001-01-",
                1000, LocalDate.of(2027, 1, 31), "Principal", false);
    }

    private FiscalRangeRequest validRequest() {
        return new FiscalRangeRequest("CAI-ABC-123", 1001, 2000, "000-001-01-",
                1000, LocalDate.of(2027, 1, 31), "Principal");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_retorna200() throws Exception {
        when(service.list(2)).thenReturn(List.of(range()));

        mockMvc.perform(get("/cajas/2/fiscal-ranges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cai").value("CAI-ABC-123"))
                .andExpect(jsonPath("$[0].enUso").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valido_retorna201() throws Exception {
        when(service.create(eq(2), any(FiscalRangeRequest.class))).thenReturn(range());

        mockMvc.perform(post("/cajas/2/fiscal-ranges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_sinCai_retorna400() throws Exception {
        FiscalRangeRequest sinCai = new FiscalRangeRequest(" ", 1001, 2000, "000-001-01-",
                1000, LocalDate.of(2027, 1, 31), null);

        mockMvc.perform(post("/cajas/2/fiscal-ranges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sinCai)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_retorna204() throws Exception {
        mockMvc.perform(delete("/cajas/2/fiscal-ranges/7"))
                .andExpect(status().isNoContent());

        verify(service).delete(2, 7);
    }
}

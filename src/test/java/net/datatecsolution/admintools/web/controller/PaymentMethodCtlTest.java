package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.PaymentMethodRequest;
import net.datatecsolution.admintools.domain.dto.PaymentMethodResponse;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.PaymentMethodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentMethodCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentMethodCtlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PaymentMethodService service;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean(name = "commonDataSource") private DataSource commonDataSource;

    @Test
    @WithMockUser
    void getAll_retorna200() throws Exception {
        when(service.getAll()).thenReturn(List.of(
                new PaymentMethodResponse(1, "Efectivo"),
                new PaymentMethodResponse(2, "Tarjeta")));

        mockMvc.perform(get("/payment-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Efectivo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valido_retorna201() throws Exception {
        when(service.create(any(PaymentMethodRequest.class)))
                .thenReturn(new PaymentMethodResponse(3, "Transferencia"));

        mockMvc.perform(post("/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentMethodRequest("Transferencia"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_descripcionVacia_retorna400() throws Exception {
        mockMvc.perform(post("/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentMethodRequest(" "))))
                .andExpect(status().isBadRequest());
    }
}

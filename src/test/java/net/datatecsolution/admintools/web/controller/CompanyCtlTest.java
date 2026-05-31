package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.CompanyRequest;
import net.datatecsolution.admintools.domain.dto.CompanyResponse;
import net.datatecsolution.admintools.domain.service.CompanyService;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyCtlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CompanyService service;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean(name = "commonDataSource") private DataSource commonDataSource;

    @Test
    @WithMockUser
    void get_retorna200() throws Exception {
        when(service.getCompany()).thenReturn(new CompanyResponse(
                1, "Mi Negocio", "08011985123456", "9999-9999", "a@b.com", "Dueno", "Centro", null));

        mockMvc.perform(get("/company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mi Negocio"))
                .andExpect(jsonPath("$.rtn").value("08011985123456"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void put_valido_retorna200() throws Exception {
        when(service.updateCompany(any(CompanyRequest.class))).thenReturn(new CompanyResponse(
                1, "Nuevo", "08011985123456", "", "", "", "", null));

        mockMvc.perform(put("/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyRequest(
                                "Nuevo", "08011985123456", "", "", "", "", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void put_rtnInvalido_retorna400() throws Exception {
        mockMvc.perform(put("/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyRequest(
                                "Nuevo", "123", "", "", "", "", null))))
                .andExpect(status().isBadRequest());
    }
}

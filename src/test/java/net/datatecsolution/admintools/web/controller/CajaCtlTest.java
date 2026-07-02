package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.dto.CajaRequest;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.domain.service.CajaAdminService;
import net.datatecsolution.admintools.domain.service.CajaCatalogService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-101 — slice del controller de cajas (lista + alta/edición ADMIN).
 */
@WebMvcTest(controllers = CajaCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class CajaCtlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CajaCatalogService catalogService;
    @MockBean private CajaAdminService adminService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean(name = "commonDataSource") private DataSource commonDataSource;

    private CajaResponse caja() {
        return new CajaResponse(9, "Caja 9", 3, "Bodega Norte", "admin_tools_caja_9");
    }

    @Test
    @WithMockUser
    void getAll_incluyeNombreDeBodega() throws Exception {
        when(catalogService.getAll()).thenReturn(List.of(caja()));

        mockMvc.perform(get("/cajas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].warehouseName").value("Bodega Norte"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_valido_retorna201() throws Exception {
        when(adminService.create(any(CajaRequest.class))).thenReturn(caja());

        mockMvc.perform(post("/cajas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CajaRequest("Caja 9", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dbName").value("admin_tools_caja_9"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_descripcionVacia_retorna400() throws Exception {
        mockMvc.perform(post("/cajas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CajaRequest(" ", 3))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_sinBodega_retorna400() throws Exception {
        mockMvc.perform(post("/cajas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CajaRequest("Caja 9", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_valido_retorna200() throws Exception {
        when(adminService.update(eq(9), any(CajaRequest.class))).thenReturn(caja());

        mockMvc.perform(put("/cajas/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CajaRequest("Caja 9", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Caja 9"));
    }
}

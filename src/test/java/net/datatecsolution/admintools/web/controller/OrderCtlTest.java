package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderCtlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    // Mocks necesarios porque @WebMvcTest carga SecurityConfig + JwtRequestFilter
    // que dependen de estos beans, aunque la cadena de filtros este desactivada
    // via addFilters = false.
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // Requerido por TenantInterceptor (HandlerInterceptor que el slice levanta);
    // su preHandle tolera getConnection() nulo, asi que el mock basta.
    @MockBean(name = "commonDataSource")
    private DataSource commonDataSource;

    private static final Principal RONAL = () -> "ronal";

    @Test
    void getById_existente_retorna200() throws Exception {
        Order order = new Order();
        order.setOrderId(42);
        when(orderService.getOrderUser(42, "ronal")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/42").param("user", "ronal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42));
    }

    @Test
    void getById_inexistente_retorna404() throws Exception {
        when(orderService.getOrderUser(eq(99), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/99").param("user", "ronal"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByToday_conPrincipal_retorna200() throws Exception {
        when(orderService.findByToday("ronal")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders/today").principal(RONAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void save_happyPath_retorna201() throws Exception {
        Order incoming = new Order();
        Order saved = new Order();
        saved.setOrderId(7);

        when(orderService.save(any(Order.class), eq("ronal"))).thenReturn(saved);

        mockMvc.perform(post("/orders/save")
                        .principal(RONAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incoming)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(7));
    }

    @Test
    void save_intentoActualizarOrdenAjena_retorna404() throws Exception {
        // Simula el ataque: usuario "ronal" envia un orderId existente
        // pero que pertenece a otro vendedor. El service debe rechazar
        // con 404 para no revelar si el id existe o no.
        Order intentoUpdate = new Order();
        intentoUpdate.setOrderId(42);

        when(orderService.save(any(Order.class), eq("ronal")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Orden no encontrada o no pertenece al usuario"));

        mockMvc.perform(post("/orders/save")
                        .principal(RONAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intentoUpdate)))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_servicioLanzaExcepcion_retorna500() throws Exception {
        when(orderService.save(any(Order.class), anyString()))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(post("/orders/save")
                        .principal(RONAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Order())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_existente_retorna200() throws Exception {
        when(orderService.delete(anyInt(), eq("ronal"), anyBoolean())).thenReturn(true);

        mockMvc.perform(delete("/orders/delete/5").principal(RONAL))
                .andExpect(status().isOk());
    }

    @Test
    void delete_inexistente_retorna404() throws Exception {
        when(orderService.delete(anyInt(), eq("ronal"), anyBoolean())).thenReturn(false);

        mockMvc.perform(delete("/orders/delete/999").principal(RONAL))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_sinFlag_esLogico() throws Exception {
        when(orderService.delete(anyInt(), eq("ronal"), anyBoolean())).thenReturn(true);

        mockMvc.perform(delete("/orders/delete/5").principal(RONAL))
                .andExpect(status().isOk());

        // POS: sin ?fisico → borrado lógico (estado 5).
        verify(orderService).delete(5, "ronal", false);
    }

    @Test
    void delete_conFlagFisico_esFisico() throws Exception {
        when(orderService.delete(anyInt(), eq("ronal"), anyBoolean())).thenReturn(true);

        mockMvc.perform(delete("/orders/delete/5?fisico=true").principal(RONAL))
                .andExpect(status().isOk());

        // App de órdenes: ?fisico=true → borrado físico de la fila.
        verify(orderService).delete(5, "ronal", true);
    }
}

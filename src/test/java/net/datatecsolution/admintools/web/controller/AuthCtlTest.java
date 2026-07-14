package net.datatecsolution.admintools.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.datatecsolution.admintools.config.JwtUtil;
import net.datatecsolution.admintools.domain.LoginRequest;
import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import net.datatecsolution.admintools.domain.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthCtl.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthCtlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    // US-049: dependencia nueva del controller (throttle anti-fuerza-bruta).
    // Mockeado → isBlocked() devuelve false por default (no bloquea); los tests
    // de credenciales no dependen del throttle. El test del 429 lo stubbea.
    @MockBean
    private LoginAttemptService loginAttemptService;

    // Requerido por TenantInterceptor (HandlerInterceptor que el slice levanta);
    // su preHandle tolera getConnection() nulo, asi que el mock basta.
    @MockBean(name = "commonDataSource")
    private DataSource commonDataSource;

    @Test
    void login_conCredencialesValidas_retornaTokenYUsername() throws Exception {
        UserDetails fakeUser = new User("ronal", "encoded", Collections.emptyList());
        when(userDetailsService.loadUserByUsername(eq("ronal"))).thenReturn(fakeUser);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("fake.jwt.token");

        LoginRequest body = new LoginRequest();
        body.setUsername("ronal");
        body.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"))
                .andExpect(jsonPath("$.username").value("ronal"));
    }

    @Test
    void login_conCredencialesInvalidas_retorna401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest body = new LoginRequest();
        body.setUsername("ronal");
        body.setPassword("wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                // US-049: mensaje genérico, sin filtrar la excepción.
                .andExpect(content().string("Credenciales inválidas"));
    }

    @Test
    void login_conThrottleActivo_retorna429() throws Exception {
        // US-049: la clave usuario+IP está bloqueada por exceso de fallos.
        when(loginAttemptService.isBlocked(any())).thenReturn(true);

        LoginRequest body = new LoginRequest();
        body.setUsername("ronal");
        body.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refresh_conTokenValido_retornaNuevoToken() throws Exception {
        UserDetails fakeUser = new User("ronal", "encoded", Collections.emptyList());
        when(jwtUtil.extractUsername("valid.token")).thenReturn("ronal");
        when(userDetailsService.loadUserByUsername("ronal")).thenReturn(fakeUser);
        when(jwtUtil.validateToken(eq("valid.token"), any(UserDetails.class))).thenReturn(true);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("new.jwt.token");

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new.jwt.token"));
    }

    @Test
    void refresh_conTokenInvalido_retorna401() throws Exception {
        UserDetails fakeUser = new User("ronal", "encoded", Collections.emptyList());
        when(jwtUtil.extractUsername("invalid.token")).thenReturn("ronal");
        when(userDetailsService.loadUserByUsername("ronal")).thenReturn(fakeUser);
        when(jwtUtil.validateToken(eq("invalid.token"), any(UserDetails.class))).thenReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }
}

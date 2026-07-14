package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
// US-049: CORS lo gobierna el bean global corsConfigurationSource() (lista por
// env var CORS_ALLOWED_ORIGINS). El @CrossOrigin hardcodeado se quitó — pisaba
// esa config justo en el endpoint de login y cableaba una IP pública al binario.
public class AuthCtl {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private net.datatecsolution.admintools.config.JwtUtil jwtUtil;

    @Autowired
    private net.datatecsolution.admintools.domain.service.CustomUserDetailsService userDetailsService;

    @Autowired
    private net.datatecsolution.admintools.domain.service.LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   jakarta.servlet.http.HttpServletRequest httpRequest) {
        // US-049: throttle anti-fuerza-bruta por usuario+IP.
        String attemptKey = (loginRequest.getUsername() == null ? "?" : loginRequest.getUsername())
                + "|" + clientIp(httpRequest);
        if (loginAttemptService.isBlocked(attemptKey)) {
            return ResponseEntity.status(429)
                    .body("Demasiados intentos fallidos. Espere unos minutos e intente de nuevo.");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            final org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                    .loadUserByUsername(loginRequest.getUsername());

            final String jwt = jwtUtil.generateToken(userDetails);

            // Rol primario (ROLE_X derivado de tipo_permiso) sin el prefijo
            // ROLE_, para que el frontend enrute por privilegio (p.ej. CASHIER).
            String role = userDetails.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .map(a -> a.substring(5))
                    .orElse("USER");

            loginAttemptService.recordSuccess(attemptKey);

            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("token", jwt);
            response.put("username", userDetails.getUsername());
            response.put("role", role);

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(attemptKey);
            // US-049: mensaje genérico — no distinguir usuario-inexistente de
            // contraseña-incorrecta ni filtrar internals de la excepción.
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }

    /** IP del cliente respetando X-Forwarded-For (primer hop) tras el proxy. */
    private static String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String username = jwtUtil.extractUsername(token);

            if (username != null) {
                final org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                        .loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    final String jwt = jwtUtil.generateToken(userDetails);
                    String role = userDetails.getAuthorities().stream()
                            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                            .filter(a -> a.startsWith("ROLE_"))
                            .findFirst()
                            .map(a -> a.substring(5))
                            .orElse("USER");
                    java.util.Map<String, String> response = new java.util.HashMap<>();
                    response.put("token", jwt);
                    response.put("username", userDetails.getUsername());
                    response.put("role", role);
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.status(401).body("Invalid token");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Error refreshing token: " + e.getMessage());
        }
    }
}

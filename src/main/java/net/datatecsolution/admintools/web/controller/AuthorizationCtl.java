package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.service.AdminAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Autorización por contraseña de administrador (gate de Precios / Descuentos en
 * la facturación táctil). Bajo {@code /authorization} → NO es público (cae en
 * {@code anyRequest().authenticated()}), así que requiere un cajero logueado;
 * evita un brute-force anónimo de contraseñas de admin.
 */
@RestController
@RequestMapping("/authorization")
@Tag(name = "Authorization", description = "Autorización de acciones protegidas (comprobarAdmin)")
public class AuthorizationCtl {

    private final AdminAuthorizationService service;

    public AuthorizationCtl(AdminAuthorizationService service) {
        this.service = service;
    }

    public record VerifyAdminRequest(String password) {}

    @PostMapping("/verify-admin")
    @Operation(summary = "Valida una contraseña contra cualquier usuario admin/supervisor (tipo_permiso 1 o 4)")
    public ResponseEntity<Map<String, Boolean>> verifyAdmin(@RequestBody VerifyAdminRequest req) {
        boolean ok = service.verifyAdminPassword(req != null ? req.password() : null);
        return ResponseEntity.ok(Map.of("authorized", ok));
    }
}

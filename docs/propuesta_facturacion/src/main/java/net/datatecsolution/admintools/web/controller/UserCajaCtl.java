package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.UserCajaResponse;
import net.datatecsolution.admintools.domain.dto.UserCajaUpsertRequest;
import net.datatecsolution.admintools.domain.service.UserCajaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4.5 fix — cajas asignadas a un usuario. Sub-recurso de /users.
 *
 * GET: cualquier autenticado (un cajero loggeado consulta sus propias
 * cajas para el switcher de la app POS).
 *
 * PUT: ADMIN. Reemplaza el set completo. Sincroniza
 * usuario.codigo_caja con la default para que el TenantInterceptor
 * resuelva bien el tenant en el siguiente login.
 */
@RestController
@RequestMapping("/users/{userId}/cajas")
@Tag(name = "User Cajas", description = "Cajas asignadas a un usuario (cajas_usuarios)")
public class UserCajaCtl {

    private final UserCajaService service;

    public UserCajaCtl(UserCajaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista las cajas asignadas al usuario (con la default marcada)")
    public ResponseEntity<List<UserCajaResponse>> get(@PathVariable int userId) {
        return ResponseEntity.ok(service.getByUser(userId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reemplaza el set completo de cajas del usuario (exactamente 1 default)")
    public ResponseEntity<List<UserCajaResponse>> replace(
            @PathVariable int userId,
            @RequestBody @Valid List<UserCajaUpsertRequest> incoming) {
        return ResponseEntity.ok(service.replaceAll(userId, incoming));
    }
}

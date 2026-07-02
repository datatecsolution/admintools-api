package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CajaRequest;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.domain.service.CajaAdminService;
import net.datatecsolution.admintools.domain.service.CajaCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4.5 fix — catalogo de cajas para frontend.
 * US-101 — alta/edicion de cajas (replica del flujo Swing CtlCajasLista/CtlCaja):
 * POST provisiona ademas la BD de la caja (CREATE DATABASE + baseline Flyway).
 * Sin DELETE, fiel al Swing.
 */
@RestController
@RequestMapping("/cajas")
@Tag(name = "Cajas", description = "Catalogo y administracion de cajas")
public class CajaCtl {

    private final CajaCatalogService service;
    private final CajaAdminService adminService;

    public CajaCtl(CajaCatalogService service, CajaAdminService adminService) {
        this.service = service;
        this.adminService = adminService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las cajas del cliente")
    public ResponseEntity<List<CajaResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/current")
    @Operation(summary = "Caja de la sesión actual (resuelta por el tenant del JWT)")
    public ResponseEntity<CajaResponse> getCurrent() {
        return service.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea una caja y provisiona su base de datos (CREATE DATABASE + migraciones)")
    public ResponseEntity<CajaResponse> create(@Valid @RequestBody CajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualiza descripción y bodega de una caja (nombre_db intacto)")
    public ResponseEntity<CajaResponse> update(@PathVariable Integer id,
                                               @Valid @RequestBody CajaRequest request) {
        return ResponseEntity.ok(adminService.update(id, request));
    }
}

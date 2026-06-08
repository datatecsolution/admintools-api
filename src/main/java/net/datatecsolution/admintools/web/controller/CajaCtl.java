package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.domain.service.CajaCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4.5 fix — catalogo de cajas para frontend.
 */
@RestController
@RequestMapping("/cajas")
@Tag(name = "Cajas", description = "Catalogo de cajas (read-only desde el panel)")
public class CajaCtl {

    private final CajaCatalogService service;

    public CajaCtl(CajaCatalogService service) {
        this.service = service;
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
}

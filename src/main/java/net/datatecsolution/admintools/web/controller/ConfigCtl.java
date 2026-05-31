package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.ConfigRequest;
import net.datatecsolution.admintools.domain.dto.ConfigResponse;
import net.datatecsolution.admintools.domain.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-031 — Parametros generales de facturacion (config_app).
 */
@RestController
@RequestMapping("/config")
@Tag(name = "Config", description = "Parametros generales (US-031)")
public class ConfigCtl {

    private final ConfigService service;

    public ConfigCtl(ConfigService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener los parametros generales")
    public ResponseEntity<ConfigResponse> get() {
        return ResponseEntity.ok(service.getConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar los parametros generales")
    public ResponseEntity<ConfigResponse> update(@Valid @RequestBody ConfigRequest request) {
        return ResponseEntity.ok(service.updateConfig(request));
    }
}

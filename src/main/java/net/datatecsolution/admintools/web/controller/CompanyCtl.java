package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CompanyRequest;
import net.datatecsolution.admintools.domain.dto.CompanyResponse;
import net.datatecsolution.admintools.domain.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-031 — Datos generales de la empresa (BD comun, single-row). El logo se
 * sube con POST /upload (US-030) y su URL llega en {@code logoUrl}.
 */
@RestController
@RequestMapping("/company")
@Tag(name = "Company", description = "Datos generales de la empresa (US-031)")
public class CompanyCtl {

    private final CompanyService service;

    public CompanyCtl(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener los datos de la empresa")
    public ResponseEntity<CompanyResponse> get() {
        return ResponseEntity.ok(service.getCompany());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar los datos de la empresa")
    public ResponseEntity<CompanyResponse> update(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(service.updateCompany(request));
    }
}

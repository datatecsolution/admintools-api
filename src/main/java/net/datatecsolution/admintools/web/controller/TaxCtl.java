package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.TaxResponse;
import net.datatecsolution.admintools.domain.service.TaxCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4.5+ fix — catalogo de impuestos para el frontend.
 *
 * El frontend (ProductFormDialog) lo necesita para el dropdown
 * "Impuesto" del modal de producto. Hasta hoy el hook useTaxes lo
 * fallbackeaba hardcoded, mostrando 404 ruidoso en consola.
 */
@RestController
@RequestMapping("/taxes")
@Tag(name = "Taxes", description = "Catalogo de impuestos (read-only)")
public class TaxCtl {

    private final TaxCatalogService service;

    public TaxCtl(TaxCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los impuestos del catalogo")
    public ResponseEntity<List<TaxResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}

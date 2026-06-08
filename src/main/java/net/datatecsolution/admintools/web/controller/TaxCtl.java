package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.TaxRequest;
import net.datatecsolution.admintools.domain.dto.TaxResponse;
import net.datatecsolution.admintools.domain.service.TaxCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogo de impuestos. El GET lo usa el frontend (dropdown de impuesto en
 * ProductFormDialog); US-032 agrega el CRUD para la pantalla de configuracion.
 */
@RestController
@RequestMapping("/taxes")
@Tag(name = "Taxes", description = "Catalogo de impuestos (US-032: CRUD)")
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un impuesto")
    public ResponseEntity<TaxResponse> create(@Valid @RequestBody TaxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un impuesto")
    public ResponseEntity<TaxResponse> update(@PathVariable int id,
                                              @Valid @RequestBody TaxRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un impuesto")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

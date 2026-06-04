package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PriceCatalogResponse;
import net.datatecsolution.admintools.domain.dto.PriceTypeRequest;
import net.datatecsolution.admintools.domain.service.PriceCatalogService;
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
 * Sprint 4.5 fix — catalogo de tipos de precio.
 *
 * NO usa /price (lo tiene PriceCtl legacy de Swing). Path /price-catalog
 * cubre lectura + CRUD admin. GET es abierto a cualquier autenticado
 * (los cajeros lo necesitan para mostrar nombres en facturacion); POST/
 * PUT/DELETE solo ADMIN.
 */
@RestController
@RequestMapping("/price-catalog")
@Tag(name = "Price Catalog", description = "Tipos de precio (extensible: Publico General, Mayoristas, etc.)")
public class PriceCatalogCtl {

    private final PriceCatalogService service;

    public PriceCatalogCtl(PriceCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los tipos de precio del catalogo (extensible)")
    public ResponseEntity<List<PriceCatalogResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un tipo de precio por id")
    public ResponseEntity<PriceCatalogResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo tipo de precio")
    public ResponseEntity<PriceCatalogResponse> create(@Valid @RequestBody PriceTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar el nombre de un tipo de precio")
    public ResponseEntity<PriceCatalogResponse> update(@PathVariable int id,
                                                       @Valid @RequestBody PriceTypeRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un tipo de precio (409 si esta en uso)")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

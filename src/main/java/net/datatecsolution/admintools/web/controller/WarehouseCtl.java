package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.WarehouseRequest;
import net.datatecsolution.admintools.domain.dto.WarehouseResponse;
import net.datatecsolution.admintools.domain.service.WarehouseService;
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
 * CRUD de warehouses (INV-3). El espejo a departamento se maneja en
 * {@link WarehouseService} y no se expone al API.
 */
@RestController
@RequestMapping("/warehouses")
@Tag(name = "Warehouses", description = "Warehouse management (mirrors automatically to departamento)")
public class WarehouseCtl {

    private final WarehouseService warehouseService;

    public WarehouseCtl(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @Operation(summary = "List all warehouses")
    public ResponseEntity<List<WarehouseResponse>> getAll() {
        return ResponseEntity.ok(warehouseService.getAll());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a warehouse by code")
    public ResponseEntity<WarehouseResponse> getById(@PathVariable int code) {
        return ResponseEntity.ok(warehouseService.getById(code));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Create a new warehouse (code assigned by the system)")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Update a warehouse description")
    public ResponseEntity<WarehouseResponse> update(@PathVariable int code,
                                                    @Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.update(code, request));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Delete a warehouse (and its departamento mirror)")
    public ResponseEntity<Void> delete(@PathVariable int code) {
        warehouseService.delete(code);
        return ResponseEntity.noContent().build();
    }
}

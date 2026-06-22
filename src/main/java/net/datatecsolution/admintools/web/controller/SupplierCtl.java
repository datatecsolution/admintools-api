package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.SupplierRequest;
import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.domain.service.SupplierService;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Maestro de proveedores (INV-5 + US-096). Lectura sin gate (la usan los
 * dropdowns de Compras/Facturación); escritura gateada a ADMIN. Migra el
 * alta/edición/baja del Swing (CtlProveedor/ProveedorDao). El DELETE responde
 * 409 si el proveedor está en uso (compras / CxP / recibos), en vez de 500.
 */
@RestController
@RequestMapping("/suppliers")
@Tag(name = "Suppliers", description = "CRUD del maestro de proveedores")
public class SupplierCtl {

    private final SupplierService service;

    public SupplierCtl(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los proveedores (plano, para dropdowns)")
    public ResponseEntity<List<SupplierResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar proveedores paginados (nombre/dirección) con saldo")
    public ResponseEntity<Page<SupplierResponse>> search(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(search, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un proveedor por id (con saldo)")
    public ResponseEntity<SupplierResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un proveedor (el id lo asigna la BD)")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un proveedor existente")
    public ResponseEntity<SupplierResponse> update(@PathVariable int id,
                                                   @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un proveedor (409 si está en uso)")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

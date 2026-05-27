package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.domain.service.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoints de proveedores (INV-5). CRUD completo se difiere
 * a una historia aparte; aqui solo lo necesario para que la UI pueda
 * listar/seleccionar proveedores al crear una compra.
 */
@RestController
@RequestMapping("/suppliers")
@Tag(name = "Suppliers", description = "Read-only supplier listing")
public class SupplierCtl {

    private final SupplierService service;

    public SupplierCtl(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all suppliers")
    public ResponseEntity<List<SupplierResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier by id")
    public ResponseEntity<SupplierResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CategoryRequest;
import net.datatecsolution.admintools.domain.dto.CategoryResponse;
import net.datatecsolution.admintools.domain.service.CategoryService;
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
 * Sprint 4 #50 — CRUD de categorias de producto.
 *
 * Backend de US-027 (frontend). Patron US-019: DTOs records,
 * @Valid + GlobalExceptionHandler para errores 400/404/409, RBAC ADMIN
 * para escritura, autenticado para lectura.
 */
@RestController
@RequestMapping("/categories")
@Tag(name = "Categories", description = "Categorias de producto (US-027 backend)")
public class CategoryCtl {

    private final CategoryService service;

    public CategoryCtl(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorias")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /** US-081 — arbol de categorias padre-hijo (marcas.parent_id, V38). */
    @GetMapping("/tree")
    @Operation(summary = "Arbol jerarquico de categorias (US-081)")
    public ResponseEntity<List<net.datatecsolution.admintools.domain.dto.CategoryTreeNode>> getTree() {
        return ResponseEntity.ok(service.getTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoria por id")
    public ResponseEntity<CategoryResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva categoria")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una categoria existente")
    public ResponseEntity<CategoryResponse> update(@PathVariable int id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una categoria (409 si tiene productos asignados)")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.dto.ProductResponse;
import net.datatecsolution.admintools.domain.service.ProductMasterService;
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

/**
 * CRUD del master de productos (INV-4). Coexiste con el legacy
 * {@code ProductCtl} (que sirve los GET de lectura para la orders app
 * de produccion). Aqui solo van los endpoints clean con DTOs y validacion
 * — paginated list + create + update + delete sobre la tabla {@code articulo}.
 *
 * No se expone {@code GET /products/{id}} porque colisionaria con el
 * legacy; los writes devuelven la {@link ProductResponse} completa, asi
 * que callers tienen lo que necesitan.
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Products (master)", description = "Master data CRUD para productos")
public class ProductMasterCtl {

    private final ProductMasterService service;

    public ProductMasterCtl(ProductMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos paginados, opcionalmente filtrados por nombre")
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(name, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Crear un producto nuevo (el id lo asigna la BD)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Actualizar un producto existente")
    public ResponseEntity<ProductResponse> update(@PathVariable int id,
                                                  @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Eliminar un producto")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

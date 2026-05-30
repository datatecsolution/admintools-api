package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.ProductPriceResponse;
import net.datatecsolution.admintools.domain.dto.ProductPriceUpsertRequest;
import net.datatecsolution.admintools.domain.service.ProductPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4.5 fix — precios por articulo. Sub-recurso de /products.
 *
 * GET: lectura abierta a cualquier autenticado (un cajero necesita
 * los precios para facturar).
 *
 * PUT: ADMIN. Reemplaza la lista completa de precios del articulo.
 * Modela el "guardar cambios" del modal de producto: el frontend
 * manda 1..N filas, el backend hace diff (upsert + delete de los
 * faltantes).
 */
@RestController
@RequestMapping("/products/{productId}/prices")
@Tag(name = "Product Prices", description = "Precios por articulo (precios_articulos)")
public class ProductPriceCtl {

    private final ProductPriceService service;

    public ProductPriceCtl(ProductPriceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los precios actuales del articulo")
    public ResponseEntity<List<ProductPriceResponse>> get(@PathVariable int productId) {
        return ResponseEntity.ok(service.getByProduct(productId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reemplaza el set completo de precios del articulo (upsert + delete)")
    public ResponseEntity<List<ProductPriceResponse>> replace(
            @PathVariable int productId,
            @RequestBody @Valid List<ProductPriceUpsertRequest> incoming) {
        return ResponseEntity.ok(service.replaceAll(productId, incoming));
    }
}

package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
// US-049: CORS lo gobierna el bean global (env CORS_ALLOWED_ORIGINS); se quitó
// el @CrossOrigin hardcodeado que lo pisaba en este controller.
public class ProductCtl {
    @Autowired
    private ProductService productService;

    @Autowired
    private net.datatecsolution.admintools.domain.service.SalesRankingService salesRankingService;

    /**
     * US-094 — Ranking de ventas de la caja actual para ordenar el catálogo:
     * categorías por unidades vendidas + top de productos ("Más vendidos").
     * Ventana en días configurable (config_app); {@code days} la sobreescribe.
     */
    @GetMapping("/sales-ranking")
    public ResponseEntity<net.datatecsolution.admintools.domain.dto.SalesRankingResponse> salesRanking(
            @RequestParam(name = "days", required = false) Integer days) {
        return ResponseEntity.ok(salesRankingService.ranking(days));
    }

    // @GetMapping("/all")
    // public ResponseEntity<List<Product>> getAll() {
    //
    // return new ResponseEntity<>(productService.getAll(), HttpStatus.OK);
    // }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable("productId") int productId) {
        return productService.getProduct(productId)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/description/{description}")
    public ResponseEntity<List<Product>> getByDescription(@PathVariable("description") String description,
            java.security.Principal principal) {
        String user = principal.getName();
        if (description == null || description.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return productService.getProductsPrecioUser(description, user)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable("categoryId") int categoryId) {
        return productService.getBycategory(categoryId)
                .map(products -> new ResponseEntity<>(products, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // US-022: removed /products/despriciouser/{description} — era un duplicado
    // con typo de /products/description/{description}; ambos llamaban a
    // productService.getProductsPrecioUser(...). Si el frontend lo seguia
    // usando, debe migrar a /products/description/{description}.

    // US-049: estos endpoints legacy de escritura del maestro no tenían gate de
    // rol → cualquier autenticado (cajero/vendedor) creaba o borraba productos.
    // El CRUD real vive en ProductMasterCtl (ADMIN). Se cierran a ADMIN por si
    // algún cliente viejo aún los invoca.
    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> save(@RequestBody Product product) {

        return new ResponseEntity<>(productService.save(product), HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity delete(@PathVariable("id") int productId) {
        if (productService.delete(productId)) {
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.CostomerLegacy;
import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.service.CustomerService;
import net.datatecsolution.admintools.domain.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * US-123 — PUENTE DE COMPATIBILIDAD para el build DESPLEGADO de la app de
 * pedidos (at-ordenes-ventas en Sharon), que quedó atrás respecto al repo.
 *
 * US-022 borró dos endpoints legacy asumiendo que ningún cliente los usaba:
 *   - GET /products/despriciouser/{d}  (duplicado con typo de /products/description/{d})
 *   - GET /costomers/name/{n}          (CostomerCtl completo)
 * El front del repo ya migró a los nuevos, pero el BUILD EN PRODUCCIÓN de
 * Sharon sigue llamando a los viejos: tras el deploy 2026-07-30 la búsqueda
 * de productos y de clientes devolvía 404 (51 y 28 llamadas en los logs del
 * proxy). Restaurarlos acá evita tocar el front en producción.
 *
 * Delegan en los MISMOS servicios que los endpoints nuevos — no duplican
 * lógica, solo reponen la ruta (y, para clientes, la forma del JSON con el
 * typo histórico `costomer*` que ese front lee).
 *
 * BORRAR esta clase cuando el front desplegado se actualice a
 * /products/description/{d} y /customers.
 */
@RestController
@Tag(name = "Legacy (app de pedidos)",
     description = "Endpoints de compatibilidad para el build viejo de at-ordenes-ventas. No usar en clientes nuevos.")
public class LegacyOrdersAppCtl {

    private final ProductService productService;
    private final CustomerService customerService;

    public LegacyOrdersAppCtl(ProductService productService, CustomerService customerService) {
        this.productService = productService;
        this.customerService = customerService;
    }

    /** @deprecated usar {@code GET /products/description/{description}}. */
    @Deprecated
    @GetMapping("/products/despriciouser/{description}")
    @Operation(summary = "[LEGACY] Búsqueda de productos por descripción y precios del usuario")
    public ResponseEntity<List<Product>> productosLegacy(@PathVariable("description") String description,
            Principal principal) {
        if (description == null || description.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return productService.getProductsPrecioUser(description, principal.getName())
                .map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /** @deprecated usar {@code GET /customers?name=}. */
    @Deprecated
    @GetMapping("/costomers/name/{name}")
    @Operation(summary = "[LEGACY] Búsqueda de clientes por nombre, filtrada por el vendedor")
    public ResponseEntity<List<CostomerLegacy>> clientesLegacy(@PathVariable("name") String name,
            Principal principal) {
        if (name == null || name.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return customerService.getdByName(name, principal.getName())
                .map(lista -> new ResponseEntity<>(lista.stream().map(CostomerLegacy::de).toList(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}

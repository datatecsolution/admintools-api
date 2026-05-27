package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PurchaseRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseResponse;
import net.datatecsolution.admintools.domain.service.PurchaseService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Crear y consultar compras (INV-5). No hay PUT/DELETE — las correcciones
 * son devoluciones de compra (INV-7).
 *
 * Al insertar las lineas, el trigger {@code detalle_compra_b_inset}
 * dispara {@code crear_compa_kardex}: el stock sube en
 * {@code existencia_articulo_bodega} sin codigo del API.
 */
@RestController
@RequestMapping("/purchases")
@Tag(name = "Purchases", description = "Crear y consultar compras")
public class PurchaseCtl {

    private final PurchaseService service;

    public PurchaseCtl(PurchaseService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVENTORY')")   // US-021
    @Operation(summary = "Crear una compra (header + lineas → stock sube via trigger)")
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request,
                                                   Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping
    @Operation(summary = "Listar compras paginadas, filtros opcionales: supplier, from, to")
    public ResponseEntity<Page<PurchaseResponse>> search(
            @RequestParam(name = "supplier", required = false) Integer supplier,
            @RequestParam(name = "from",     required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to",       required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page",     defaultValue = "0") int page,
            @RequestParam(name = "size",     defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(supplier, from, to, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una compra por id (header + lineas)")
    public ResponseEntity<PurchaseResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }
}

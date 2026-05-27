package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnLineResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnResponse;
import net.datatecsolution.admintools.domain.service.PurchaseReturnService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Devoluciones de compra al proveedor (INV-7). Cada item, al
 * persistirse, dispara el trigger del kardex que BAJA el stock en
 * la bodega + actualiza {@code existencia_articulo_bodega}.
 */
@RestController
@RequestMapping("/purchase-returns")
@Tag(name = "Purchase Returns", description = "Devoluciones de compra al proveedor (stock baja)")
public class PurchaseReturnCtl {

    private final PurchaseReturnService service;

    public PurchaseReturnCtl(PurchaseReturnService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVENTORY')")   // US-021
    @Operation(summary = "Crear devolucion de una compra (uno o varios items en una transaccion)")
    public ResponseEntity<PurchaseReturnResponse> create(@Valid @RequestBody PurchaseReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar items de devolucion paginados (filtros: purchase, from, to)")
    public ResponseEntity<Page<PurchaseReturnLineResponse>> search(
            @RequestParam(name = "purchase", required = false) Integer purchase,
            @RequestParam(name = "from",     required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to",       required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page",     defaultValue = "0") int page,
            @RequestParam(name = "size",     defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(purchase, from, to, page, size));
    }

    @GetMapping("/by-purchase/{purchaseId}")
    @Operation(summary = "Items de devolucion asociados a una compra original (sin paginar)")
    public ResponseEntity<List<PurchaseReturnLineResponse>> getByPurchase(@PathVariable int purchaseId) {
        return ResponseEntity.ok(service.getByPurchase(purchaseId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Item de devolucion individual por codigo_devolucion")
    public ResponseEntity<PurchaseReturnLineResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }
}

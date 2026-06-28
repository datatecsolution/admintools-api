package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.InventoryCountRequest;
import net.datatecsolution.admintools.domain.dto.InventoryCountResponse;
import net.datatecsolution.admintools.domain.service.InventoryCountService;
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
import java.time.LocalDateTime;

/**
 * Actas de toma física: registro persistente de cada inventario físico
 * (encabezado + detalle). Aparte de los movimientos del cierre.
 */
@RestController
@RequestMapping("/inventory/counts")
@Tag(name = "Inventory counts", description = "Actas de toma física (registro)")
public class InventoryCountCtl {

    private final InventoryCountService service;

    public InventoryCountCtl(InventoryCountService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVENTORY')")
    @Operation(summary = "Registrar un acta de toma física (encabezado + detalle)")
    public ResponseEntity<InventoryCountResponse> create(
            @Valid @RequestBody InventoryCountRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Listar actas de toma física (paginado; filtros opcionales)")
    public ResponseEntity<Page<InventoryCountResponse>> search(
            @RequestParam(name = "warehouse", required = false) Integer warehouse,
            @RequestParam(name = "from", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(warehouse, from, to, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Obtener un acta por id (encabezado + líneas)")
    public ResponseEntity<InventoryCountResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }
}

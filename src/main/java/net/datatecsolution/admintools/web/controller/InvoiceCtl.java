package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.InvoiceCreateRequest;
import net.datatecsolution.admintools.domain.dto.InvoiceListItem;
import net.datatecsolution.admintools.domain.dto.InvoiceResponse;
import net.datatecsolution.admintools.domain.service.InvoiceAdminQueryService;
import net.datatecsolution.admintools.domain.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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

/**
 * INV-8 / US-020 — Facturacion definitiva. La caja destino se resuelve via
 * {@code TenantContext} desde el JWT del request (US-017).
 *
 * Todas las rutas requieren un usuario con caja asignada
 * ({@code usuario.codigo_caja > 0}); 403 si no.
 */
@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices", description = "Facturas definitivas por caja (INV-8 / US-020)")
public class InvoiceCtl {

    private final InvoiceService service;
    private final InvoiceAdminQueryService adminQuery;

    public InvoiceCtl(InvoiceService service, InvoiceAdminQueryService adminQuery) {
        this.service = service;
        this.adminQuery = adminQuery;
    }

    /**
     * US-041 — Sección Facturas (admin/supervisor): lista las facturas de una
     * CAJA elegida (no del tenant), más reciente primero, con filtros por
     * estado (ACT/NULA/ALL, default ACT), rango de fechas y nombre de cliente.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    @Operation(summary = "Listar facturas de una caja (filtros: estado, fechas, cliente)")
    public ResponseEntity<Page<InvoiceListItem>> adminList(
            @RequestParam("caja") int caja,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "from", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "estado", defaultValue = "ACT") String estado,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(adminQuery.list(caja, search, from, to, estado, PageRequest.of(page, size)));
    }

    @PostMapping("/from-order/{orderId}")
    @PreAuthorize("hasRole('CASHIER')")   // US-021
    @Operation(summary = "Crear factura desde una orden temporal — descuenta inventario via trigger")
    public ResponseEntity<InvoiceResponse> createFromOrder(@PathVariable int orderId,
                                                           Principal principal) {
        InvoiceResponse created = service.createFromOrder(orderId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping
    @PreAuthorize("hasRole('CASHIER')")
    @Operation(summary = "Checkout POS: crear factura directo desde el carrito (sin orden), contado o credito")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceCreateRequest request,
                                                  Principal principal) {
        InvoiceResponse created = service.createDirect(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Obtener una factura por numero_factura de la caja del usuario")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable int invoiceId) {
        return ResponseEntity.ok(service.getById(invoiceId));
    }

    @GetMapping
    @Operation(summary = "Listar facturas paginadas de la caja del usuario")
    public ResponseEntity<Page<InvoiceResponse>> search(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(page, size));
    }
}

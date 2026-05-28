package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.SaleReturnLineResponse;
import net.datatecsolution.admintools.domain.dto.SaleReturnRequest;
import net.datatecsolution.admintools.domain.dto.SaleReturnResponse;
import net.datatecsolution.admintools.domain.service.SaleReturnService;
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
 * Sale Returns (devoluciones de venta). El cliente regresa mercaderia;
 * el stock sube en la bodega de la caja via trigger
 * {@code detalle_devolucion_b_inset} + {@code crear_dev_venta_kardex}
 * (V19/V20 fixed). El API NO toca kardex.
 *
 * Caja se infiere del JWT (TenantContext, US-017). Sin caja asignada -> 403.
 */
@RestController
@RequestMapping("/sale-returns")
@Tag(name = "Sale Returns", description = "Devoluciones de venta — el cliente regresa mercaderia")
public class SaleReturnCtl {

    private final SaleReturnService service;

    public SaleReturnCtl(SaleReturnService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('CASHIER')")   // US-021
    @Operation(summary = "Crear una devolucion de venta (uno o varios items en una transaccion)")
    public ResponseEntity<SaleReturnResponse> create(@Valid @RequestBody SaleReturnRequest request,
                                                     Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping
    @Operation(summary = "Listar lineas de devolucion paginadas (filtros: invoice, from, to)")
    public ResponseEntity<Page<SaleReturnLineResponse>> search(
            @RequestParam(name = "invoice", required = false) Integer invoice,
            @RequestParam(name = "from",    required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to",      required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page",    defaultValue = "0") int page,
            @RequestParam(name = "size",    defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(invoice, from, to, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una linea de devolucion por codigo_devolucion")
    public ResponseEntity<SaleReturnLineResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/by-invoice/{invoiceNumber}")
    @Operation(summary = "Listar todas las devoluciones agrupadas de una factura especifica")
    public ResponseEntity<SaleReturnResponse> findByInvoice(@PathVariable int invoiceNumber) {
        return ResponseEntity.ok(service.findByInvoice(invoiceNumber));
    }
}

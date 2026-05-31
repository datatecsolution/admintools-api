package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.AbonoRequest;
import net.datatecsolution.admintools.domain.dto.BalanceResponse;
import net.datatecsolution.admintools.domain.dto.DelinquentResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceAccountResponse;
import net.datatecsolution.admintools.domain.dto.LedgerEntryResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptResponse;
import net.datatecsolution.admintools.domain.service.AccountsReceivableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;

/**
 * US-033 — Modulo de cuentas por cobrar. Patron del Sprint 3/4: DTOs,
 * inyeccion por constructor, errores via GlobalExceptionHandler, seguridad
 * con @PreAuthorize y documentacion OpenAPI.
 *
 * Las cuentas por cobrar viven en la BD comun (no per-caja), por eso estos
 * endpoints no requieren caja asignada.
 */
@RestController
@RequestMapping("/accounts-receivable")
@Tag(name = "Cuentas por cobrar", description = "Saldos, estado de cuenta, morosidad y abonos (US-033)")
public class AccountsReceivableCtl {

    private final AccountsReceivableService service;

    public AccountsReceivableCtl(AccountsReceivableService service) {
        this.service = service;
    }

    @GetMapping("/{customerId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','SELLER')")
    @Operation(summary = "Saldo actual de cuentas por cobrar del cliente")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable int customerId) {
        return ResponseEntity.ok(service.getBalance(customerId));
    }

    @GetMapping("/{customerId}/statement")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','SELLER')")
    @Operation(summary = "Estado de cuenta del cliente (movimientos), paginado")
    public ResponseEntity<Page<LedgerEntryResponse>> getStatement(
            @PathVariable int customerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getStatement(customerId, PageRequest.of(page, size)));
    }

    @GetMapping("/{customerId}/invoices")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','SELLER')")
    @Operation(summary = "Estado por factura: saldos pendientes de cada factura a credito del cliente")
    public ResponseEntity<List<InvoiceAccountResponse>> getInvoiceAccounts(@PathVariable int customerId) {
        return ResponseEntity.ok(service.getInvoiceAccounts(customerId));
    }

    @GetMapping("/delinquent")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Reporte de morosidad: facturas con saldo y al menos minDays dias de atraso")
    public ResponseEntity<Page<DelinquentResponse>> listDelinquent(
            @RequestParam(name = "minDays", defaultValue = "30") int minDays,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listDelinquent(minDays, PageRequest.of(page, size)));
    }

    @PostMapping("/{customerId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Aplicar un abono a nivel cliente (genera recibo y baja el saldo)")
    public ResponseEntity<ReceiptResponse> applyPayment(@PathVariable int customerId,
                                                        @Valid @RequestBody AbonoRequest request,
                                                        Principal principal) {
        String user = principal != null ? principal.getName() : null;
        ReceiptResponse receipt = service.applyPayment(customerId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping("/{customerId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','SELLER')")
    @Operation(summary = "Historial de recibos de pago del cliente, paginado")
    public ResponseEntity<Page<ReceiptResponse>> getReceipts(
            @PathVariable int customerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getReceipts(customerId, PageRequest.of(page, size)));
    }

    // ---- US-034: cobro y trazabilidad a nivel factura ----

    @PostMapping("/invoices/{codigoCuenta}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Aplicar un abono a una factura a credito (cobro)")
    public ResponseEntity<ReceiptResponse> applyInvoicePayment(@PathVariable int codigoCuenta,
                                                              @Valid @RequestBody AbonoRequest request,
                                                              Principal principal) {
        String user = principal != null ? principal.getName() : null;
        ReceiptResponse receipt = service.applyInvoicePayment(codigoCuenta, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping("/invoices/{codigoCuenta}/statement")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER','SELLER')")
    @Operation(summary = "Historial de movimientos de una factura a credito")
    public ResponseEntity<Page<LedgerEntryResponse>> getInvoiceStatement(
            @PathVariable int codigoCuenta,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getInvoiceStatement(codigoCuenta, PageRequest.of(page, size)));
    }
}

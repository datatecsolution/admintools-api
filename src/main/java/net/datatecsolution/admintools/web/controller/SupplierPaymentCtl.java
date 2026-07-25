package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PaymentAccountResponse;
import net.datatecsolution.admintools.domain.dto.SupplierBalanceResponse;
import net.datatecsolution.admintools.domain.dto.SupplierPaymentRequest;
import net.datatecsolution.admintools.domain.dto.SupplierReceiptDetailResponse;
import net.datatecsolution.admintools.domain.dto.SupplierReceiptResponse;
import net.datatecsolution.admintools.domain.service.SupplierPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Cuenta por pagar (pago a proveedores) del POS — réplica de CtlPagoProveedor.
 * El gate hasAnyRole('ADMIN','SELLER') deja entrar al cajero por jerarquía
 * (CASHIER > SELLER), igual que el resto de operaciones de caja.
 */
@RestController
@Tag(name = "Pago a proveedores", description = "Cuenta por pagar: saldo, formas de pago y registro de pago")
public class SupplierPaymentCtl {

    private final SupplierPaymentService service;

    public SupplierPaymentCtl(SupplierPaymentService service) {
        this.service = service;
    }

    @GetMapping("/suppliers/payable")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Proveedores con saldo pendiente (cuenta por pagar)")
    public ResponseEntity<List<SupplierBalanceResponse>> payable() {
        return ResponseEntity.ok(service.withBalance());
    }

    @GetMapping("/payment-accounts")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Formas de pago (efectivo de caja + cuentas de banco)")
    public ResponseEntity<List<PaymentAccountResponse>> paymentAccounts() {
        return ResponseEntity.ok(service.paymentAccounts());
    }

    @PostMapping("/suppliers/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Registra un pago a proveedor (recibo + débito CxP)")
    public ResponseEntity<SupplierReceiptResponse> pay(@PathVariable int id,
                                                       @Valid @RequestBody SupplierPaymentRequest request,
                                                       Principal principal) {
        String user = principal != null ? principal.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED).body(service.pay(id, request, user));
    }

    @GetMapping("/suppliers/receipts/{noRecibo}")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @Operation(summary = "Re-lectura de un recibo de proveedor para reimprimir su comprobante (US-108)")
    public ResponseEntity<SupplierReceiptDetailResponse> getReceipt(@PathVariable int noRecibo) {
        return ResponseEntity.ok(service.getReceipt(noRecibo));
    }
}

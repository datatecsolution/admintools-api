package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PaymentMethodRequest;
import net.datatecsolution.admintools.domain.dto.PaymentMethodResponse;
import net.datatecsolution.admintools.domain.service.PaymentMethodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * US-032 — CRUD del catalogo de metodos de pago (tabla tipo_pago).
 */
@RestController
@RequestMapping("/payment-methods")
@Tag(name = "Payment methods", description = "Metodos de pago (US-032)")
public class PaymentMethodCtl {

    private final PaymentMethodService service;

    public PaymentMethodCtl(PaymentMethodService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista los metodos de pago")
    public ResponseEntity<List<PaymentMethodResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un metodo de pago")
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un metodo de pago")
    public ResponseEntity<PaymentMethodResponse> update(@PathVariable int id,
                                                        @Valid @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un metodo de pago")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

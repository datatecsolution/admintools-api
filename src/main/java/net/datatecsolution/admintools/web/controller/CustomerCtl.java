package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Controller de clientes con el patron nuevo del Sprint 3:
 * DTOs (entrada/salida), inyeccion por constructor, errores via
 * GlobalExceptionHandler, validacion con @Valid y documentacion OpenAPI.
 *
 * US-022 cleanup: el legacy CostomerCtl (/costomers) fue eliminado;
 * este es ahora el unico controller de clientes.
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Clientes", description = "Gestion de clientes")
public class CustomerCtl {

    private final CustomerService customerService;

    public CustomerCtl(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Buscar clientes del vendedor autenticado, paginado")
    public ResponseEntity<Page<CustomerResponse>> search(
            @RequestParam(name = "name", required = false, defaultValue = "") String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Principal principal) {
        Page<CustomerResponse> result =
                customerService.search(name, principal.getName(), PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un cliente por su id")
    public ResponseEntity<CustomerResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PostMapping
    // El POS (cajero/vendedor) crea clientes contado; el tipo 2 (crédito) queda
    // gated en el servicio por config crear_cliente_credito o rol ADMIN. El
    // payload legacy del panel (sin tipoCliente) sigue siendo solo ADMIN.
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Crear un cliente para el vendedor autenticado")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request,
                                                   org.springframework.security.core.Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        CustomerResponse created = customerService.create(request, auth.getName(), isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Actualizar un cliente existente")
    public ResponseEntity<CustomerResponse> update(@PathVariable int id,
                                                   @Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    @Operation(summary = "Eliminar un cliente")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

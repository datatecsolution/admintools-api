package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.service.CostomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Convive temporalmente con el antiguo {@code CostomerCtl} (/costomers) mientras
 * el frontend migra. El viejo se elimina en el commit de cutover (US-019).
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Clientes", description = "Gestion de clientes")
public class CustomerCtl {

    private final CostomerService customerService;

    public CustomerCtl(CostomerService customerService) {
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
    @Operation(summary = "Crear un cliente para el vendedor autenticado")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request,
                                                   Principal principal) {
        CustomerResponse created = customerService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un cliente existente")
    public ResponseEntity<CustomerResponse> update(@PathVariable int id,
                                                   @Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un cliente")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

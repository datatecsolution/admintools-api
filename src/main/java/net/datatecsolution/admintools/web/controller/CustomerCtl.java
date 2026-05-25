package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.service.CostomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de clientes con el patron nuevo del Sprint 3:
 * DTOs de salida, inyeccion por constructor, errores via GlobalExceptionHandler
 * y documentacion OpenAPI.
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

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un cliente por su id")
    public ResponseEntity<CustomerResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(customerService.getById(id));
    }
}

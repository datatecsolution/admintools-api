package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.SellerSettingsResponse;
import net.datatecsolution.admintools.domain.service.SellerCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Catálogo de vendedores + config (ventana_vendedor) para el checkout del POS.
 */
@RestController
@RequestMapping("/sellers")
@Tag(name = "Sellers", description = "Vendedores (empleados) y config de vendedor para el POS")
public class SellerCtl {

    private final SellerCatalogService service;

    public SellerCtl(SellerCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Vendedores y si el usuario debe elegir vendedor al facturar")
    public ResponseEntity<SellerSettingsResponse> get(Principal principal) {
        String user = principal != null ? principal.getName() : "";
        return ResponseEntity.ok(service.getSettings(user));
    }
}

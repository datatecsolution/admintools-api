package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.InvoiceAdminDetailResponse;
import net.datatecsolution.admintools.domain.service.InvoiceAdminQueryService;
import net.datatecsolution.admintools.domain.service.InvoiceQrTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * US-100 — consulta PÚBLICA de una factura para el autoservicio por QR
 * (permitAll en SecurityConfig, como el GET de imagen de producto). El
 * acceso está protegido por el token HMAC del QR: token inválido o feature
 * apagada → 404 indistinguible de factura inexistente (no filtra existencia).
 * El shape es el del detalle admin (sin datos de costo).
 */
@RestController
@RequestMapping("/public/invoices")
@Tag(name = "Public", description = "Factura pública por QR (US-100)")
public class PublicInvoiceCtl {

    private final InvoiceQrTokenService tokenService;
    private final InvoiceAdminQueryService queryService;

    public PublicInvoiceCtl(InvoiceQrTokenService tokenService,
                            InvoiceAdminQueryService queryService) {
        this.tokenService = tokenService;
        this.queryService = queryService;
    }

    @GetMapping("/{caja}/{numero}")
    @Operation(summary = "Detalle de factura vía token del QR (sin autenticación)")
    public ResponseEntity<InvoiceAdminDetailResponse> get(@PathVariable int caja,
                                                          @PathVariable int numero,
                                                          @RequestParam(name = "t") String token) {
        if (!tokenService.matches(caja, numero, token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada");
        }
        return ResponseEntity.ok(queryService.detail(caja, numero));
    }
}

package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.PublicInvoiceResponse;
import net.datatecsolution.admintools.domain.service.CompanyService;
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
    private final CompanyService companyService;

    public PublicInvoiceCtl(InvoiceQrTokenService tokenService,
                            InvoiceAdminQueryService queryService,
                            CompanyService companyService) {
        this.tokenService = tokenService;
        this.queryService = queryService;
        this.companyService = companyService;
    }

    @GetMapping("/{caja}/{numero}")
    @Operation(summary = "Detalle de factura + membrete vía token del QR (sin autenticación)")
    public ResponseEntity<PublicInvoiceResponse> get(@PathVariable int caja,
                                                     @PathVariable int numero,
                                                     @RequestParam(name = "t") String token) {
        if (!tokenService.matches(caja, numero, token)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada");
        }
        // El membrete viaja DENTRO de la respuesta gateada por el token: la
        // página pública imprime con membrete sin abrir GET /company al mundo.
        // toPublic() recorta el login del cajero y las facturas restantes del
        // rango fiscal — datos que el cliente final no necesita ver.
        return ResponseEntity.ok(new PublicInvoiceResponse(
                queryService.detail(caja, numero).toPublic(), companyService.getCompany()));
    }
}

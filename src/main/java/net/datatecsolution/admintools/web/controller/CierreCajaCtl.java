package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.AperturaCajaRequest;
import net.datatecsolution.admintools.domain.dto.CierreActualResponse;
import net.datatecsolution.admintools.domain.dto.CierreCajaRequest;
import net.datatecsolution.admintools.domain.dto.CierreResumenResponse;
import net.datatecsolution.admintools.domain.service.CierreCajaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Cierre de caja (turno) del POS — replica del flujo del Swing:
 * apertura al entrar a facturacion (contar efectivo inicial) y cierre con
 * arqueo al terminar el turno. El usuario se resuelve del JWT.
 */
@RestController
@RequestMapping("/cierre-caja")
@Tag(name = "Cierre de caja", description = "Apertura y cierre del turno (arqueo de efectivo)")
public class CierreCajaCtl {

    private final CierreCajaService service;

    public CierreCajaCtl(CierreCajaService service) {
        this.service = service;
    }

    @GetMapping("/actual")
    @Operation(summary = "Estado del turno actual del usuario (abierto/cerrado)")
    public ResponseEntity<CierreActualResponse> actual(Principal principal) {
        return ResponseEntity.ok(service.getActual(user(principal)));
    }

    @PostMapping("/apertura")
    @Operation(summary = "Abre el turno con el efectivo inicial contado")
    public ResponseEntity<CierreActualResponse> abrir(@Valid @RequestBody AperturaCajaRequest request,
                                                      Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrir(request, user(principal)));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen del turno para el cuadre (preview del cierre)")
    public ResponseEntity<CierreResumenResponse> resumen(Principal principal) {
        return ResponseEntity.ok(service.resumen(user(principal)));
    }

    @PostMapping
    @Operation(summary = "Cierra el turno con el efectivo contado del arqueo")
    public ResponseEntity<CierreActualResponse> cerrar(@Valid @RequestBody CierreCajaRequest request,
                                                       Principal principal) {
        return ResponseEntity.ok(service.cerrar(request, user(principal)));
    }

    private static String user(Principal principal) {
        return principal != null ? principal.getName() : "SYSTEM";
    }
}

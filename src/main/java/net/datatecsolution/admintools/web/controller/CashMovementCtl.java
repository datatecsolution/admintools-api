package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.CashMovementRequest;
import net.datatecsolution.admintools.domain.dto.CashMovementResponse;
import net.datatecsolution.admintools.domain.service.CierreCajaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Movimientos de efectivo de la caja (entradas/salidas) — mirror de
 * {@code CtlEntradaCaja}/{@code CtlSalidaCaja} del Swing. Requiere turno
 * abierto; el cierre los agrega por rango de numero + usuario.
 */
@RestController
@RequestMapping("/cash-movements")
@Tag(name = "Movimientos de caja", description = "Entradas y salidas de efectivo del turno")
public class CashMovementCtl {

    private final CierreCajaService service;

    public CashMovementCtl(CierreCajaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra una entrada o salida de efectivo")
    public ResponseEntity<CashMovementResponse> create(@Valid @RequestBody CashMovementRequest request,
                                                       Principal principal) {
        String user = principal != null ? principal.getName() : "SYSTEM";
        int id = service.registrarMovimiento(request, user);
        // US-108: mismos campos que el GET de re-lectura (el POS arma el
        // comprobante del response); el POS viejo solo lee `id` y sigue OK.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.getMovimiento(request.tipo(), id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Re-lectura de un movimiento para reimprimir su comprobante (US-108)")
    public ResponseEntity<CashMovementResponse> get(@PathVariable int id,
                                                    @RequestParam("tipo") String tipo) {
        return ResponseEntity.ok(service.getMovimiento(tipo, id));
    }
}

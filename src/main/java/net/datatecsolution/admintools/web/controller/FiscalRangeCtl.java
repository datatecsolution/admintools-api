package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.FiscalRangeRequest;
import net.datatecsolution.admintools.domain.dto.FiscalRangeResponse;
import net.datatecsolution.admintools.domain.dto.FiscalRangesResponse;
import net.datatecsolution.admintools.domain.service.FiscalRangeService;
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
 * US-101 — Datos de facturación (CAI/rangos fiscales) por caja, réplica de
 * CtlDatosFacturacionLista/CtlDatosFacturacion del Swing. Configuración
 * fiscal: todo gateado a ADMIN. POST/PUT reposicionan el AUTO_INCREMENT de
 * encabezado_factura de la caja (la próxima factura arranca el rango).
 */
@RestController
@RequestMapping("/cajas/{cajaId}/fiscal-ranges")
@Tag(name = "Rangos fiscales", description = "Datos de facturación CAI/SAR por caja")
public class FiscalRangeCtl {

    private final FiscalRangeService service;

    public FiscalRangeCtl(FiscalRangeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rangos fiscales (datos_factura) de la caja + último número emitido")
    public ResponseEntity<FiscalRangesResponse> list(@PathVariable int cajaId) {
        return ResponseEntity.ok(service.list(cajaId));
    }

    @GetMapping("/{rangeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Detalle de un rango fiscal")
    public ResponseEntity<FiscalRangeResponse> get(@PathVariable int cajaId, @PathVariable int rangeId) {
        return ResponseEntity.ok(service.get(cajaId, rangeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un rango fiscal y arranca la numeración (AUTO_INCREMENT = factura inicial)")
    public ResponseEntity<FiscalRangeResponse> create(@PathVariable int cajaId,
                                                      @Valid @RequestBody FiscalRangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(cajaId, request));
    }

    @PutMapping("/{rangeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualiza un rango fiscal (re-aplica la numeración)")
    public ResponseEntity<FiscalRangeResponse> update(@PathVariable int cajaId, @PathVariable int rangeId,
                                                      @Valid @RequestBody FiscalRangeRequest request) {
        return ResponseEntity.ok(service.update(cajaId, rangeId, request));
    }

    @DeleteMapping("/{rangeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un rango fiscal sin facturas emitidas (409 si está en uso)")
    public ResponseEntity<Void> delete(@PathVariable int cajaId, @PathVariable int rangeId) {
        service.delete(cajaId, rangeId);
        return ResponseEntity.noContent().build();
    }
}

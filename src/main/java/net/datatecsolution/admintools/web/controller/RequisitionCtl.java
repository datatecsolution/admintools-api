package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.RequisitionRequest;
import net.datatecsolution.admintools.domain.dto.RequisitionResponse;
import net.datatecsolution.admintools.domain.service.RequisitionService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Crear y consultar requisiciones — transferencias de stock entre
 * bodegas (INV-6). Caso comun: merma de la bodega de trabajo a "Perdidas".
 *
 * Al insertar las lineas, el trigger {@code d_requisicion_b_insert}
 * dispara los SPs de salida (origen) + entrada (destino):
 * stock baja en origen, sube en destino, ambos UPSERT-ean
 * {@code existencia_articulo_bodega} en la misma transaccion.
 */
@RestController
@RequestMapping("/requisitions")
@Tag(name = "Requisitions", description = "Transferencias de stock entre bodegas (mermas a Perdidas, traslados, etc.)")
public class RequisitionCtl {

    private final RequisitionService service;

    public RequisitionCtl(RequisitionService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVENTORY')")   // US-021
    @Operation(summary = "Crear una requisicion (origen → destino; el kardex se actualiza via trigger)")
    public ResponseEntity<RequisitionResponse> create(@Valid @RequestBody RequisitionRequest request,
                                                      Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping
    @Operation(summary = "Listar requisiciones paginadas; filtros opcionales: origin, destination, from, to")
    public ResponseEntity<Page<RequisitionResponse>> search(
            @RequestParam(name = "origin",      required = false) Integer origin,
            @RequestParam(name = "destination", required = false) Integer destination,
            @RequestParam(name = "from",        required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to",          required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(name = "page",        defaultValue = "0") int page,
            @RequestParam(name = "size",        defaultValue = "20") int size) {
        return ResponseEntity.ok(service.search(origin, destination, from, to, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una requisicion por id (header + lineas)")
    public ResponseEntity<RequisitionResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }
}

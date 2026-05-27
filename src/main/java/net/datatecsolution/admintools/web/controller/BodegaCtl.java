package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.BodegaRequest;
import net.datatecsolution.admintools.domain.dto.BodegaResponse;
import net.datatecsolution.admintools.domain.service.BodegaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * CRUD de bodegas (INV-3). El espejo a departamento se maneja en
 * {@code BodegaService} y no se expone al API.
 */
@RestController
@RequestMapping("/bodegas")
@Tag(name = "Bodegas", description = "Gestion de bodegas (mantiene espejo automatico con departamento)")
public class BodegaCtl {

    private final BodegaService bodegaService;

    public BodegaCtl(BodegaService bodegaService) {
        this.bodegaService = bodegaService;
    }

    @GetMapping
    @Operation(summary = "Listar todas las bodegas")
    public ResponseEntity<List<BodegaResponse>> getAll() {
        return ResponseEntity.ok(bodegaService.getAll());
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtener una bodega por codigo")
    public ResponseEntity<BodegaResponse> getById(@PathVariable int codigo) {
        return ResponseEntity.ok(bodegaService.getById(codigo));
    }

    @PostMapping
    @Operation(summary = "Crear una bodega nueva (codigo asignado por el sistema)")
    public ResponseEntity<BodegaResponse> create(@Valid @RequestBody BodegaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bodegaService.create(request));
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Actualizar la descripcion de una bodega")
    public ResponseEntity<BodegaResponse> update(@PathVariable int codigo,
                                                 @Valid @RequestBody BodegaRequest request) {
        return ResponseEntity.ok(bodegaService.update(codigo, request));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Eliminar una bodega (y su espejo en departamento)")
    public ResponseEntity<Void> delete(@PathVariable int codigo) {
        bodegaService.delete(codigo);
        return ResponseEntity.noContent().build();
    }
}

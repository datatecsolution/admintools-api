package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.ExistenciaResponse;
import net.datatecsolution.admintools.domain.service.ExistenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de lectura del subsistema de inventario (INV-1).
 *
 * Vive bajo {@code /inventario} (no /products) para no chocar con el legacy
 * {@code ProductCtl}. Lee siempre detras de {@link ExistenciaService}, que
 * a su vez consulta la tabla {@code existencia_articulo_bodega} mantenida
 * transaccionalmente por los SPs del kardex (V19/V20).
 */
@RestController
@RequestMapping("/inventario")
@Tag(name = "Inventario", description = "Lectura de existencias por articulo y bodega")
public class InventarioCtl {

    private final ExistenciaService existenciaService;

    public InventarioCtl(ExistenciaService existenciaService) {
        this.existenciaService = existenciaService;
    }

    @GetMapping("/existencia")
    @Operation(summary = "Saldo actual de un articulo en una bodega especifica")
    public ResponseEntity<ExistenciaResponse> getExistencia(
            @RequestParam("articulo") int articulo,
            @RequestParam(name = "bodega", defaultValue = "1") int bodega) {
        return ResponseEntity.ok(existenciaService.getExistencia(articulo, bodega));
    }

    @GetMapping("/articulo/{id}/existencias")
    @Operation(summary = "Saldos del articulo en todas las bodegas donde tiene kardex")
    public ResponseEntity<List<ExistenciaResponse>> getExistenciasPorArticulo(
            @PathVariable("id") int codigoArticulo) {
        return ResponseEntity.ok(existenciaService.getExistenciasPorArticulo(codigoArticulo));
    }
}

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.WarehouseRequest;
import net.datatecsolution.admintools.domain.dto.WarehouseResponse;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DepartamentoCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.Departamento;
import net.datatecsolution.admintools.persistence.mapper.WarehouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD de Warehouses (INV-3). Mantiene el espejo transaccional con
 * Departamento: ambas tablas deben tener entradas con el mismo codigo,
 * porque los triggers de requisicion del kardex usan
 * codigo_depart_origen/destino como sinonimo de codigo_bodega.
 *
 * Los codigos se asignan a mano como max(MAX(bodega),MAX(departamento))+1
 * (mismo patron de V17) para evitar colisiones entre las dos secuencias
 * auto_increment independientes.
 */
@Service
public class WarehouseService {

    @Autowired private BodegaCRUD bodegaCRUD;
    @Autowired private DepartamentoCRUD departamentoCRUD;
    @Autowired private WarehouseMapper mapper;

    public List<WarehouseResponse> getAll() {
        return mapper.toResponses(bodegaCRUD.findAll());
    }

    public WarehouseResponse getById(int code) {
        Bodega b = bodegaCRUD.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse " + code + " not found"));
        return mapper.toResponse(b);
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        int newCode = Math.max(
                bodegaCRUD.findMaxCodigoBodega(),
                departamentoCRUD.findMaxCodigoDepartamento()
        ) + 1;
        Bodega bodega = new Bodega(newCode, request.description());
        Departamento departamento = new Departamento(newCode, request.description());
        bodegaCRUD.save(bodega);
        departamentoCRUD.save(departamento);
        return mapper.toResponse(bodega);
    }

    @Transactional
    public WarehouseResponse update(int code, WarehouseRequest request) {
        Bodega bodega = bodegaCRUD.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse " + code + " not found"));
        bodega.setDescripcionBodega(request.description());
        bodegaCRUD.save(bodega);
        // mantener espejo: si el departamento existe lo actualizamos; si no
        // (estado inconsistente heredado), lo creamos para reparar el espejo.
        Departamento depto = departamentoCRUD.findById(code)
                .orElseGet(() -> new Departamento(code, null));
        depto.setNombre(request.description());
        departamentoCRUD.save(depto);
        return mapper.toResponse(bodega);
    }

    @Transactional
    public void delete(int code) {
        if (!bodegaCRUD.existsById(code)) {
            throw new EntityNotFoundException("Warehouse " + code + " not found");
        }
        // proteccion basica: no permitir borrar Tienda Principal (codigo 1).
        // Un borrado mas sofisticado (cascada controlada o "deshabilitar")
        // seria una mejora futura.
        if (code == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete warehouse 1 (Main Store)");
        }
        bodegaCRUD.deleteById(code);
        departamentoCRUD.findById(code).ifPresent(departamentoCRUD::delete);
    }
}

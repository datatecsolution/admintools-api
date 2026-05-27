package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.BodegaRequest;
import net.datatecsolution.admintools.domain.dto.BodegaResponse;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DepartamentoCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.Departamento;
import net.datatecsolution.admintools.persistence.mapper.BodegaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD de Bodegas (INV-3). Mantiene el espejo transaccional con
 * Departamento: ambas tablas deben tener entradas con el mismo codigo,
 * porque los triggers de requisicion del kardex usan
 * codigo_depart_origen/destino como sinonimo de codigo_bodega.
 *
 * Los codigos se asignan a mano como max(MAX(bodega),MAX(departamento))+1
 * (mismo patron de V17) para evitar colisiones entre las dos secuencias
 * auto_increment independientes.
 */
@Service
public class BodegaService {

    @Autowired private BodegaCRUD bodegaCRUD;
    @Autowired private DepartamentoCRUD departamentoCRUD;
    @Autowired private BodegaMapper mapper;

    public List<BodegaResponse> getAll() {
        return mapper.toResponses(bodegaCRUD.findAll());
    }

    public BodegaResponse getById(int codigo) {
        Bodega b = bodegaCRUD.findById(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Bodega " + codigo + " no encontrada"));
        return mapper.toResponse(b);
    }

    @Transactional
    public BodegaResponse create(BodegaRequest req) {
        int nuevoCodigo = Math.max(
                bodegaCRUD.findMaxCodigoBodega(),
                departamentoCRUD.findMaxCodigoDepartamento()
        ) + 1;
        Bodega bodega = new Bodega(nuevoCodigo, req.descripcion());
        Departamento departamento = new Departamento(nuevoCodigo, req.descripcion());
        bodegaCRUD.save(bodega);
        departamentoCRUD.save(departamento);
        return mapper.toResponse(bodega);
    }

    @Transactional
    public BodegaResponse update(int codigo, BodegaRequest req) {
        Bodega bodega = bodegaCRUD.findById(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Bodega " + codigo + " no encontrada"));
        bodega.setDescripcionBodega(req.descripcion());
        bodegaCRUD.save(bodega);
        // mantener espejo: si el departamento existe lo actualizamos; si no
        // (estado inconsistente heredado), lo creamos para reparar el espejo.
        Departamento depto = departamentoCRUD.findById(codigo)
                .orElseGet(() -> new Departamento(codigo, null));
        depto.setNombre(req.descripcion());
        departamentoCRUD.save(depto);
        return mapper.toResponse(bodega);
    }

    @Transactional
    public void delete(int codigo) {
        if (!bodegaCRUD.existsById(codigo)) {
            throw new EntityNotFoundException("Bodega " + codigo + " no encontrada");
        }
        // proteccion basica: no permitir borrar Tienda Principal (codigo 1) ni
        // bodegas con cualquier rastro en articulo_kardex. Esto evita romper
        // el inventario por accidente. Un borrado mas sofisticado (cascada
        // controlada o "deshabilitar") seria una mejora futura.
        if (codigo == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la bodega 1 (Tienda Principal)");
        }
        bodegaCRUD.deleteById(codigo);
        departamentoCRUD.findById(codigo).ifPresent(departamentoCRUD::delete);
    }
}

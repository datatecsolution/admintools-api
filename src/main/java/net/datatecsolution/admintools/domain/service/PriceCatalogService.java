package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PriceCatalogResponse;
import net.datatecsolution.admintools.domain.dto.PriceTypeRequest;
import net.datatecsolution.admintools.persistence.crud.PrecioCRUD;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.entity.Precio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5 fix — catalogo de tipos de precio.
 *
 * Inicialmente solo lectura (4 tipos seed), Sprint 4.5 amplio con CRUD
 * para que un admin pueda agregar/editar/eliminar tipos sin SQL. El
 * delete valida 409 si hay precios_articulos referenciando el tipo
 * (la BD no tiene FK formal, pero igual prevenimos data huerfana).
 */
@Service
public class PriceCatalogService {

    private final PrecioCRUD crud;
    private final PreciosArticuloCRUD pricesUsage;

    public PriceCatalogService(PrecioCRUD crud, PreciosArticuloCRUD pricesUsage) {
        this.crud = crud;
        this.pricesUsage = pricesUsage;
    }

    public List<PriceCatalogResponse> getAll() {
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .sorted((a, b) -> a.getCodigoPrecio().compareTo(b.getCodigoPrecio()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PriceCatalogResponse getById(int id) {
        return crud.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de precio " + id + " no existe"));
    }

    public PriceCatalogResponse create(PriceTypeRequest req) {
        Precio p = new Precio();
        p.setDescripcion(req.name().trim());
        return toResponse(crud.save(p));
    }

    public PriceCatalogResponse update(int id, PriceTypeRequest req) {
        Precio p = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de precio " + id + " no existe"));
        p.setDescripcion(req.name().trim());
        return toResponse(crud.save(p));
    }

    public void delete(int id) {
        Precio p = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de precio " + id + " no existe"));
        long uses = pricesUsage.countByPrecioId(id);
        if (uses > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: hay " + uses + " articulo(s) con este tipo de precio asignado");
        }
        crud.delete(p);
    }

    // ----- helpers -----

    private PriceCatalogResponse toResponse(Precio p) {
        return new PriceCatalogResponse(p.getCodigoPrecio(), p.getDescripcion());
    }
}

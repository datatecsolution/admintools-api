package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.TaxRequest;
import net.datatecsolution.admintools.domain.dto.TaxResponse;
import net.datatecsolution.admintools.persistence.crud.ImpuestoCRUD;
import net.datatecsolution.admintools.persistence.entity.Impuesto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Catalogo de impuestos. US-032 lo convierte en CRUD (antes solo GET) para la
 * pantalla de configuracion: ISV 15/18/exonerado son filas editables.
 */
@Service
public class TaxCatalogService {

    private final ImpuestoCRUD crud;

    public TaxCatalogService(ImpuestoCRUD crud) {
        this.crud = crud;
    }

    public List<TaxResponse> getAll() {
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // US-032: CRUD desde el panel admin.
    public TaxResponse create(TaxRequest req) {
        Impuesto i = new Impuesto();
        i.setDescripcion(req.description().trim());
        i.setPorcentaje(req.percent().toPlainString());
        return toResponse(crud.save(i));
    }

    public TaxResponse update(int id, TaxRequest req) {
        Impuesto i = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impuesto " + id + " no encontrado"));
        i.setDescripcion(req.description().trim());
        i.setPorcentaje(req.percent().toPlainString());
        return toResponse(crud.save(i));
    }

    public void delete(int id) {
        if (!crud.existsById(id)) {
            throw new EntityNotFoundException("Impuesto " + id + " no encontrado");
        }
        crud.deleteById(id);
    }

    private TaxResponse toResponse(Impuesto i) {
        return new TaxResponse(i.getId(), i.getDescripcion(), parsePercent(i.getPorcentaje()));
    }

    /** La columna `porcentaje` es varchar(255) legacy; toleramos null/no-numerico devolviendo 0. */
    private BigDecimal parsePercent(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

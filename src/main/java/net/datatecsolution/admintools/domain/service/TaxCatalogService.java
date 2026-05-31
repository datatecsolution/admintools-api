package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.TaxResponse;
import net.datatecsolution.admintools.persistence.crud.ImpuestoCRUD;
import net.datatecsolution.admintools.persistence.entity.Impuesto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5+ fix — catalogo de impuestos (read-only).
 *
 * Mientras el cliente no necesite CRUD desde la UI, solo expone GET.
 * Si en algun momento hace falta admin, copiar el patron de
 * PriceCatalogService (POST/PUT/DELETE + 409 si tiene articulos
 * referenciando).
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

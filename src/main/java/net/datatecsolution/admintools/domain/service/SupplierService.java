package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.persistence.crud.ProveedorCRUD;
import net.datatecsolution.admintools.persistence.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only service de proveedores (INV-5). CRUD completo se difiere.
 */
@Service
public class SupplierService {

    @Autowired private ProveedorCRUD crud;
    @Autowired private SupplierMapper mapper;

    public List<SupplierResponse> getAll() {
        return mapper.toResponses(crud.findAll());
    }

    public SupplierResponse getById(int id) {
        return crud.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Supplier " + id + " not found"));
    }
}

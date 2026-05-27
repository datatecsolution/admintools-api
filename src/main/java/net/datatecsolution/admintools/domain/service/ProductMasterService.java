package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.dto.ProductResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import net.datatecsolution.admintools.persistence.mapper.ProductMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD del master de productos (INV-4). Trabaja contra la tabla
 * {@code articulo} via {@link ArticuloMaster} (entidad escribible),
 * NO contra {@code articulo_view} (que es @Immutable).
 *
 * El stock NO se gestiona aqui: vive en
 * {@code existencia_articulo_bodega} y se consulta por {@code StockService}.
 */
@Service
public class ProductMasterService {

    @Autowired private ArticuloMasterCRUD crud;
    @Autowired private ProductMasterMapper mapper;

    public Page<ProductResponse> search(String name, int page, int size) {
        Page<ArticuloMaster> result = (name == null || name.isBlank())
                ? crud.findAll(PageRequest.of(page, size))
                : crud.findByArticuloContaining(name, PageRequest.of(page, size));
        return result.map(mapper::toResponse);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        ArticuloMaster entity = mapper.toEntity(request);
        return mapper.toResponse(crud.save(entity));
    }

    @Transactional
    public ProductResponse update(int id, ProductRequest request) {
        ArticuloMaster entity = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product " + id + " not found"));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(crud.save(entity));
    }

    @Transactional
    public void delete(int id) {
        if (!crud.existsById(id)) {
            throw new EntityNotFoundException("Product " + id + " not found");
        }
        crud.deleteById(id);
    }
}

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
import org.springframework.data.domain.Sort;
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
        // Sprint 4.5+ fix: orden por id DESC para que los productos recien
        // creados aparezcan en la primera pagina. Antes MySQL devolvia el
        // orden fisico (por PK clustered ascendente) y un articulo nuevo
        // con id 319697 quedaba sepultado en la pagina ~31900.
        PageRequest pageReq = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "codigoArticulo"));
        Page<ArticuloMaster> result = (name == null || name.isBlank())
                ? crud.findAll(pageReq)
                : crud.findByArticuloContaining(name, pageReq);
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

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.ProductPriceResponse;
import net.datatecsolution.admintools.domain.dto.ProductPriceUpsertRequest;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.PrecioCRUD;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.entity.Precio;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5 fix — precios de un articulo (tabla precios_articulos).
 *
 * Un articulo tiene 1..N filas en precios_articulos, una por tipo de
 * precio (PG, CE, Mayoristas, Costos). Aqui se exponen GET (lista
 * actual) y PUT (reemplazo completo).
 *
 * PUT lo modela como un upsert: cada fila viene con (priceTypeId,
 * value); las que NO vienen pero existen en BD se borran. Asi el
 * frontend puede mandar todo el set y no preocuparse del diff.
 *
 * Validaciones:
 *   - articulo existe → 404 si no.
 *   - priceTypeId existe en `precios` → 400 si referencia un tipo
 *     inventado (FK formal previene esto a nivel DB, pero damos
 *     mensaje claro).
 *   - duplicado de priceTypeId en el request → 400.
 */
@Service
public class ProductPriceService {

    private final PreciosArticuloCRUD pricesCrud;
    private final ArticuloMasterCRUD articulosCrud;
    private final PrecioCRUD priceCatalogCrud;

    public ProductPriceService(PreciosArticuloCRUD pricesCrud,
                               ArticuloMasterCRUD articulosCrud,
                               PrecioCRUD priceCatalogCrud) {
        this.pricesCrud = pricesCrud;
        this.articulosCrud = articulosCrud;
        this.priceCatalogCrud = priceCatalogCrud;
    }

    public List<ProductPriceResponse> getByProduct(int productId) {
        ensureProductExists(productId);
        Map<Integer, String> typeNames = loadTypeNames();
        return pricesCrud.findByArticuloId(productId).stream()
                .sorted((a, b) -> a.getPrecioId().compareTo(b.getPrecioId()))
                .map(p -> toResponse(p, typeNames))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ProductPriceResponse> replaceAll(int productId, List<ProductPriceUpsertRequest> incoming) {
        ensureProductExists(productId);
        Map<Integer, String> typeNames = loadTypeNames();

        // 1) Validar payload: ids existentes + sin duplicados.
        for (ProductPriceUpsertRequest row : incoming) {
            if (!typeNames.containsKey(row.priceTypeId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "priceTypeId " + row.priceTypeId() + " no existe en el catalogo");
            }
        }
        if (incoming.stream().map(ProductPriceUpsertRequest::priceTypeId).distinct().count() != incoming.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "priceTypeId duplicado en el request");
        }

        // 2) Indexar existentes por priceTypeId para diff in-place.
        List<PrecioArticulo> existing = pricesCrud.findByArticuloId(productId);
        Map<Integer, PrecioArticulo> byType = existing.stream()
                .collect(Collectors.toMap(PrecioArticulo::getPrecioId, p -> p));

        // 3) Upsert: actualizar valores; insertar los nuevos.
        for (ProductPriceUpsertRequest row : incoming) {
            PrecioArticulo current = byType.remove(row.priceTypeId());
            if (current == null) {
                PrecioArticulo nu = new PrecioArticulo();
                nu.setArticuloId(productId);
                nu.setPrecioId(row.priceTypeId());
                nu.setPrecioArticulo(row.value());
                pricesCrud.save(nu);
            } else {
                current.setPrecioArticulo(row.value());
                pricesCrud.save(current);
            }
        }

        // 4) Borrar los que quedaron sin upsert (no vinieron en el request).
        if (!byType.isEmpty()) {
            pricesCrud.deleteAll(byType.values());
        }

        return getByProduct(productId);
    }

    // ----- helpers -----

    private void ensureProductExists(int productId) {
        if (!articulosCrud.existsById(productId)) {
            throw new EntityNotFoundException("Producto " + productId + " no existe");
        }
    }

    private Map<Integer, String> loadTypeNames() {
        Map<Integer, String> m = new HashMap<>();
        StreamSupport.stream(priceCatalogCrud.findAll().spliterator(), false)
                .forEach(p -> m.put(p.getCodigoPrecio(), p.getDescripcion()));
        return m;
    }

    private ProductPriceResponse toResponse(PrecioArticulo p, Map<Integer, String> typeNames) {
        return new ProductPriceResponse(
                p.getId(),
                p.getArticuloId(),
                p.getPrecioId(),
                typeNames.getOrDefault(p.getPrecioId(), "?"),
                p.getPrecioArticulo()
        );
    }
}

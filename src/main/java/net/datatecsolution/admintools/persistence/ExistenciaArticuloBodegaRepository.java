package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Stock;
import net.datatecsolution.admintools.domain.repository.StockRepository;
import net.datatecsolution.admintools.persistence.crud.ExistenciaArticuloBodegaCRUD;
import net.datatecsolution.admintools.persistence.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Impl de {@link StockRepository} contra la tabla materializada
 * {@code existencia_articulo_bodega}. Lecturas O(1) por PK compuesta.
 *
 * El nombre de la clase refleja la entidad (siguiendo el patron
 * ArticuloRepository implements ProductRepository, ClienteRepository
 * implements CustomerRepository).
 */
@Repository
public class ExistenciaArticuloBodegaRepository implements StockRepository {

    @Autowired
    private ExistenciaArticuloBodegaCRUD crud;

    @Autowired
    private StockMapper mapper;

    @Override
    public BigDecimal getStock(int productCode, int warehouseCode) {
        return crud.findByCodigoArticuloAndCodigoBodega(productCode, warehouseCode)
                .map(e -> e.getCantidad())
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Stock getStockDetail(int productCode, int warehouseCode) {
        return crud.findByCodigoArticuloAndCodigoBodega(productCode, warehouseCode)
                .map(mapper::toStock)
                .orElse(null);
    }

    @Override
    public List<Stock> getStockByProduct(int productCode) {
        return mapper.toStocks(crud.findByCodigoArticulo(productCode));
    }
}

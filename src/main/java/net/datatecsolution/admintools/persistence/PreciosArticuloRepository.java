package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.PriceProduct;
import net.datatecsolution.admintools.domain.repository.PricesProductRepository;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import net.datatecsolution.admintools.persistence.mapper.PriceProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PreciosArticuloRepository implements PricesProductRepository {
    @Autowired
    private PreciosArticuloCRUD preciosArticuloCRUD;
    @Autowired
    private PriceProductMapper mapper;

    @Override
    public List<PriceProduct> getAll() {
        List<PrecioArticulo> precioArticulos = (List<PrecioArticulo>) preciosArticuloCRUD.findAll();
        return mapper.toPricesProduct(precioArticulos);
    }

    @Override
    public Optional<PriceProduct> getPricesProduct(int productId) {
        return Optional.empty();
    }

    @Override
    public PriceProduct save(PriceProduct priceProduct) {
        return null;
    }

    @Override
    public void delete(int priceProductId) {

    }
}

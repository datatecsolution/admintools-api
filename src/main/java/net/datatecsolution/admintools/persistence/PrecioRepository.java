package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Price;
import net.datatecsolution.admintools.domain.repository.PriceRepository;
import net.datatecsolution.admintools.persistence.crud.PrecioCRUD;
import net.datatecsolution.admintools.persistence.entity.Precio;
import net.datatecsolution.admintools.persistence.mapper.PriceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PrecioRepository implements PriceRepository {
    @Autowired
    private PrecioCRUD precioCRUD;
    @Autowired
    private PriceMapper mapper;

    @Override
    public List<Price> getAll() {
        List<Precio> prices = (List<Precio>) precioCRUD.findAll();
        return mapper.toPrices(prices);
    }

    @Override
    public Optional<Price> getPricesProduct(int productId) {
        return Optional.empty();
    }

    @Override
    public Price save(Price price) {
        Precio precio = mapper.toPrecio(price);
        return mapper.toPrice(precioCRUD.save(precio));
    }


    @Override
    public void delete(int priceProductId) {

    }
}

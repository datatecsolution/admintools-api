package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Price;

import java.util.List;
import java.util.Optional;

public interface PriceRepository {
    List<Price> getAll();


    Optional<Price> getPricesProduct(int productId);

    Price save(Price price);

    void delete(int priceId);
}

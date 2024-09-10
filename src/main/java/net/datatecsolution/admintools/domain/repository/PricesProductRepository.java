package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.PriceProduct;

import java.util.List;
import java.util.Optional;

public interface PricesProductRepository {
    List<PriceProduct> getAll();


    Optional<PriceProduct> getPricesProduct(int productId);

    PriceProduct save(PriceProduct priceProduct);

    void delete(int priceProductId);
}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Price;
import net.datatecsolution.admintools.domain.repository.PriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceService {
    @Autowired
    private PriceRepository priceRepository;

    public List<Price> getAll() {
        return priceRepository.getAll();
    }

    public Price save(Price price) {
        return priceRepository.save(price);
    }
    /*

    public Optional<PriceProduct> getProduct(int productId) {
        return pricesProductRepository.getProduct(productId);
    }

    public Optional<List<PriceProduct>> getBycategory(int categoryId) {
        return pricesProductRepository.getByCategory(categoryId);
    }

    public Product save(PriceProduct priceProduct) {
        return pricesProductRepository.save(priceProduct);
    }

    public boolean delete(int priceProductID) {

        if(getProduct(productId).isPresent()){
            productRepository.delete(productId);
            return true;
        }else{
            return false;
        }
        return getProduct(productId).map(product -> {
            pricesProductRepository.delete(productId);
            return true;
        }).orElse(false);
    }*/
}

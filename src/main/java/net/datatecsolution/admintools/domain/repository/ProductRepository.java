package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> getAll();

    Optional<List<Product>> getByCategory(int categoryId);

    Optional<Product> getProduct(int productId);

    Optional<List<Product>> getByDescription(String description);

    Product save(Product product);

    void delete(int productId);

    Optional<List<Product>> getByDescriptionAndUser(String description, String user);
}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CajaVendedorService cajaVendedorService;

    public List<Product> getAll() {
        return productRepository.getAll();
    }

    public Optional<Product> getProduct(int productId) {
        return productRepository.getProduct(productId);
    }

    public Optional<List<Product>> getProducts(String description) {
        logger.debug("Ejecutando consulta para obtener artículos con descripción que contiene: {}", description);

        Optional <List<Product>>  articulos = productRepository.getByDescription(description);

        if (articulos.isPresent()) {
            List<Product> products = articulos.get();
            for (Product product : products) {
                logger.debug("Product ID: {}, Description: {}, Stock: {}",
                        product.getProductId(), product.getName(), product.getStock());
            }
        } else {
            logger.debug("No se encontraron productos con la descripción proporcionada.");
        }
        return productRepository.getByDescription(description);
    }

    public Optional<List<Product>> getBycategory(int categoryId) {
        return productRepository.getByCategory(categoryId);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public boolean delete(int productId) {
        /*
        if(getProduct(productId).isPresent()){
            productRepository.delete(productId);
            return true;
        }else{
            return false;
        }*/
        return getProduct(productId).map(product -> {
            productRepository.delete(productId);
            return true;
        }).orElse(false);
    }


    public Optional<List<Product>> getProductsPrecioUser(String description, String user) {

        // US-130: la busqueda muestra el disponible en la bodega de la caja
        // del vendedor — el MISMO numero que el guard del save va a validar.
        int bodega = cajaVendedorService.bodegaParaBusqueda(user);

        return productRepository.getByDescriptionAndUser(description, user, bodega);
    }
}

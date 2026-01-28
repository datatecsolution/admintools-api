package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.repository.ProductRepository;
import net.datatecsolution.admintools.persistence.crud.ArticuloCRUD;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.entity.Articulo;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import net.datatecsolution.admintools.persistence.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ArticuloRepository implements ProductRepository {
    private static final Logger logger = LoggerFactory.getLogger(ArticuloRepository.class);

    @Autowired
    private ArticuloCRUD articuloCRUD;

    @Autowired
    private PreciosArticuloCRUD preciosArticuloCRUD;

    @Autowired
    private ProductMapper mapper;

    @Override
    public List<Product> getAll() {
        List<Articulo> articulos = (List<Articulo>) articuloCRUD.findAll();
        return mapper.toProducts(articulos);
    }

    @Override
    public Optional<List<Product>> getByCategory(int categoryId) {
        //List<Articulo> articulos = articuloCRUD.findByIdCategoriaOrderByArticuloAsc(categoryId);
        List<Articulo> articulos = articuloCRUD.getArticuloCategoria(categoryId);
        return Optional.of(mapper.toProducts(articulos));
    }

    @Override
    public Optional<Product> getProduct(int productId) {
        return articuloCRUD.findById(productId).map(articulo -> mapper.toProduct(articulo));
    }

    @Override
    public Optional<List<Product>> getByDescription(String description) {
        logger.debug("Ejecutando consulta para obtener artículo con descripción: {}", description);

        //List<Articulo> articulos = articuloCRUD.findByArticuloLikeOrderByArticuloAsc("%"+description+"%");
        //List<Articulo> articulos = articuloCRUD.findByArticuloContaining("%"+description+"%");

        List<Articulo> articulos = articuloCRUD.findByArticuloContaining(description);
        for ( Articulo articulo : articulos ) {
            logger.debug("Resultado: CodigoArticulo={}, Existencia={}",
                    articulo.getArticuloId(), articulo.getExistencia());
        }

        return Optional.of(mapper.toProducts(articulos));
    }

//    public Optional<List<List<Object>>> getByDescription(String description) {
//        List<Tuple> tuples = articuloCRUD.getArticuloDescripcion(description);
//        List<List<Object>> results = new ArrayList<>();
//
//        for (Tuple tuple : tuples) {
//            List<Object> row = new ArrayList<>();
//            row.add(tuple.get("codigo_articulo"));
//            row.add(tuple.get("articulo"));
//            row.add(tuple.get("codigo_marca"));
//            row.add(tuple.get("cod_articulo"));
//            row.add(tuple.get("codigo_impuesto"));
//            row.add(tuple.get("precio_articulo"));
//            row.add(tuple.get("tipo_articulo"));
//            row.add(tuple.get("estado"));
//            row.add(tuple.get("existencia"));
//            results.add(row);
//        }
//
//        return Optional.of(results);
//    }


    @Override
    public Product save(Product product) {
        Articulo articulo = mapper.toArticulo(product);
        return mapper.toProduct(articuloCRUD.save(articulo));
    }

    public Articulo save(Articulo articulo) {
        return articuloCRUD.save(articulo);
    }

    @Override
    public void delete(int codigo) {
        articuloCRUD.deleteById(codigo);
    }

    @Override
    public Optional<List<Product>> getByDescriptionAndUser(String description, String user) {

        List<Articulo> articulos = articuloCRUD.findByDescripcionAndUser(description,user);

        // si verifica que el resultado de la busqueda tenga item para buscar los precios correctos
        if ( articulos.size() > 0 ) {

            for (Articulo articulo : articulos) {

                List<PrecioArticulo> precios = new ArrayList<>();

                //se buscan los precios que puede aplicar el usuario
                precios = preciosArticuloCRUD.findPrecioUser(articulo.getArticuloId(), user);

                //si los precios existen se aplican al articulo
                if (precios != null) {
                    articulo.setPrecioArticulos(precios);
                } else {
                    articulo.getPrecioArticulos().clear();
                    articulo.setPrecioArticulos(new ArrayList<>());
                }
            }
        }

        return Optional.of(mapper.toProducts(articulos));
    }
}

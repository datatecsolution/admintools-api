package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.persistence.entity.Articulo;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, TaxMapper.class, PriceProductMapper.class})
public interface ProductMapper {
    @Mappings({

            @Mapping(source = "articuloId", target = "productId"),
            @Mapping(source = "articulo", target = "name"),
            @Mapping(source = "precioVenta", target = "price"),
            @Mapping(source = "idCategoria", target = "categoryId"),
            @Mapping(source = "codigoImpuesto", target = "taxId"),
            @Mapping(source = "estado", target = "active"),
            @Mapping(source = "existencia", target = "stock"),
            @Mapping(source = "categoria", target = "category"),
            @Mapping(source = "imp", target = "tax"),
            @Mapping(source = "precioArticulos", target = "prices"),


    })
    Product toProduct(Articulo articulo);

    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "tipoArticulo", ignore = true),
            //@Mapping(target = "posicionPrecio", ignore = true),
            @Mapping(target = "codigoBarra", ignore = true),
            //@Mapping(target = "existencia", ignore = true)

    })
    Articulo toArticulo(Product product);

    List<Product> toProducts(List<Articulo> articulos);

}

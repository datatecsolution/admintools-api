package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.PriceProduct;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring",uses = {PriceMapper.class})
public interface PriceProductMapper {
    @Mappings({
            @Mapping(source = "id", target = "priceProductId"),
            @Mapping(source = "precioArticulo", target = "priceProduct"),
           // @Mapping(source = "art", target = "product"),
            @Mapping(source = "pre", target = "price"),
            @Mapping(source = "articuloId", target = "productId"),
            @Mapping(source = "precioId", target = "priceId"),
    })
    PriceProduct toPriceProducto(PrecioArticulo precioArticulo);

    @InheritInverseConfiguration
    PrecioArticulo toPrecioArticulo(PriceProduct priceProduct);

    List<PriceProduct> toPricesProduct(List<PrecioArticulo> precioArticulos);
}

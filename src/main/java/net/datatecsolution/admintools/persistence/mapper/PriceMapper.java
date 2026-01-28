package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Price;
import net.datatecsolution.admintools.persistence.entity.Precio;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring" , uses = {UserPriceMapper.class})
public interface PriceMapper {

    @Mappings({
            @Mapping(source = "codigoPrecio", target = "priceId"),
            @Mapping(source = "descripcion", target = "description"),
            //@Mapping(source = "precioArticulos", target = "priceProducts"),
            @Mapping(source = "usuarioPrecios", target = "userPrices"),
    })
    Price toPrice(Precio precio);

    @InheritInverseConfiguration
    Precio toPrecio(Price price);

    List<Price> toPrices(List<Precio> precios);
}

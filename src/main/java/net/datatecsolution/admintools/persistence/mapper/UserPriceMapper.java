package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.UserPrice;
import net.datatecsolution.admintools.persistence.entity.UsuarioPrecio;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserPriceMapper {
    @Mappings({
            @Mapping(source = "id", target = "userPriceId"),
           // @Mapping(source = "use", target = "us"),
            //@Mapping(source = "prec", target = "price"),
            @Mapping(source = "usuarioId", target = "userId"),
            @Mapping(source = "codigoPrecio", target = "priceId"),

    })
    UserPrice toUserPrice(UsuarioPrecio usuarioPrecio);

    @InheritInverseConfiguration
    UsuarioPrecio toUsuarioPrecio(UserPrice userPrice);

    List<UserPrice> toUserPrices(List<UsuarioPrecio> usuarioPrecios);
}

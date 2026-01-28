package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.persistence.entity.Empleado;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SellerMapper {

    @Mappings({
            @Mapping(source = "codigo", target = "id"),
            @Mapping(source = "nombre", target = "name"),
            @Mapping(source = "apellido", target = "lastName"),
            @Mapping(source = "telefono", target = "email"),
            @Mapping(source = "correo", target = "phone"),
            @Mapping(source = "direccion", target = "address"),
    })
    Seller toSeller(Empleado empleado);

    @InheritInverseConfiguration
    Empleado toEmpleado(Seller seller);

    List<Seller> toSellers(List<Seller> sellers);
}

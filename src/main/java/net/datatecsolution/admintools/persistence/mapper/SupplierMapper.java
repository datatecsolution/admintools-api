package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.persistence.entity.Proveedor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mappings({
            @Mapping(source = "codigoProveedor",  target = "id"),
            @Mapping(source = "nombreProveedor",  target = "name"),
            @Mapping(source = "telefono",         target = "phone"),
            @Mapping(source = "celular",          target = "mobile"),
            @Mapping(source = "direccion",        target = "address")
    })
    SupplierResponse toResponse(Proveedor proveedor);

    List<SupplierResponse> toResponses(List<Proveedor> proveedores);
}

package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.SupplierRequest;
import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.persistence.entity.Proveedor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mappings({
            @Mapping(source = "codigoProveedor",  target = "id"),
            @Mapping(source = "nombreProveedor",  target = "name"),
            @Mapping(source = "telefono",         target = "phone"),
            @Mapping(source = "celular",          target = "mobile"),
            @Mapping(source = "direccion",        target = "address"),
            @Mapping(target = "balance",          ignore = true)
    })
    SupplierResponse toResponse(Proveedor proveedor);

    List<SupplierResponse> toResponses(List<Proveedor> proveedores);

    @Mappings({
            @Mapping(target = "codigoProveedor",  ignore = true), // lo asigna la BD
            @Mapping(source = "name",    target = "nombreProveedor"),
            @Mapping(source = "phone",   target = "telefono"),
            @Mapping(source = "mobile",  target = "celular"),
            @Mapping(source = "address", target = "direccion")
    })
    Proveedor toEntity(SupplierRequest request);

    @Mappings({
            @Mapping(target = "codigoProveedor",  ignore = true), // PK inmutable
            @Mapping(source = "name",    target = "nombreProveedor"),
            @Mapping(source = "phone",   target = "telefono"),
            @Mapping(source = "mobile",  target = "celular"),
            @Mapping(source = "address", target = "direccion")
    })
    void updateEntity(SupplierRequest request, @MappingTarget Proveedor entity);
}

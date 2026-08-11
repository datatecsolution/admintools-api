package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.dto.ProductResponse;
import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductMasterMapper {

    /** entity → DTO de salida. MapStruct convierte Double → BigDecimal. */
    @Mappings({
            @Mapping(source = "codigoArticulo", target = "id"),
            @Mapping(source = "articulo",       target = "name"),
            @Mapping(source = "precioArticulo", target = "price"),
            @Mapping(source = "codigoMarca",    target = "categoryId"),
            @Mapping(source = "codigoImpuesto", target = "taxId"),
            @Mapping(source = "codArticulo",    target = "altCode"),
            @Mapping(source = "tipoArticulo",   target = "type"),
            @Mapping(source = "estado",         target = "active"),
            @Mapping(target = "barcodes",       ignore = true),
            // US-141: lo adjunta el service solo en el listado y solo si la
            // peticion trae bodega; en alta/edicion no aplica.
            @Mapping(target = "stock",          ignore = true)
    })
    ProductResponse toResponse(ArticuloMaster entity);

    /** DTO de entrada → entity nueva (para POST). id queda en null para autoincrement. */
    @Mappings({
            @Mapping(target = "codigoArticulo", ignore = true),
            @Mapping(source = "name",       target = "articulo"),
            @Mapping(source = "price",      target = "precioArticulo"),
            @Mapping(source = "categoryId", target = "codigoMarca"),
            @Mapping(source = "taxId",      target = "codigoImpuesto"),
            @Mapping(source = "altCode",    target = "codArticulo"),
            @Mapping(source = "type",       target = "tipoArticulo"),
            @Mapping(source = "active",     target = "estado")
    })
    ArticuloMaster toEntity(ProductRequest request);

    /** Pisa los campos del DTO sobre una entity existente (para PUT). Preserva id. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "codigoArticulo", ignore = true),
            @Mapping(source = "name",       target = "articulo"),
            @Mapping(source = "price",      target = "precioArticulo"),
            @Mapping(source = "categoryId", target = "codigoMarca"),
            @Mapping(source = "taxId",      target = "codigoImpuesto"),
            @Mapping(source = "altCode",    target = "codArticulo"),
            @Mapping(source = "type",       target = "tipoArticulo"),
            @Mapping(source = "active",     target = "estado")
    })
    void updateEntity(ProductRequest request, @MappingTarget ArticuloMaster entity);
}

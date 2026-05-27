package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.WarehouseResponse;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mappings({
            @Mapping(source = "codigoBodega", target = "code"),
            @Mapping(source = "descripcionBodega", target = "description")
    })
    WarehouseResponse toResponse(Bodega bodega);

    List<WarehouseResponse> toResponses(List<Bodega> bodegas);
}

package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.BodegaResponse;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BodegaMapper {

    @Mappings({
            @Mapping(source = "codigoBodega", target = "codigo"),
            @Mapping(source = "descripcionBodega", target = "descripcion")
    })
    BodegaResponse toResponse(Bodega bodega);

    List<BodegaResponse> toResponses(List<Bodega> bodegas);
}

package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Existencia;
import net.datatecsolution.admintools.persistence.entity.ExistenciaArticuloBodega;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExistenciaMapper {

    @Mappings({
            @Mapping(source = "codigoArticulo", target = "codigoArticulo"),
            @Mapping(source = "codigoBodega", target = "codigoBodega"),
            @Mapping(source = "bodega.descripcionBodega", target = "descripcionBodega"),
            @Mapping(source = "cantidad", target = "cantidad")
    })
    Existencia toExistencia(ExistenciaArticuloBodega entity);

    List<Existencia> toExistencias(List<ExistenciaArticuloBodega> entities);
}

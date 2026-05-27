package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Stock;
import net.datatecsolution.admintools.persistence.entity.ExistenciaArticuloBodega;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mappings({
            @Mapping(source = "codigoArticulo", target = "productCode"),
            @Mapping(source = "codigoBodega", target = "warehouseCode"),
            @Mapping(source = "bodega.descripcionBodega", target = "warehouseDescription"),
            @Mapping(source = "cantidad", target = "quantity")
    })
    Stock toStock(ExistenciaArticuloBodega entity);

    List<Stock> toStocks(List<ExistenciaArticuloBodega> entities);
}

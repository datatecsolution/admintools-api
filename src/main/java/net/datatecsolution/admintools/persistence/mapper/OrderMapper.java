package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.persistence.entity.Orden;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CostomerMapper.class,OrderDetailsMapper.class})
public interface OrderMapper {

    @Mappings({
            @Mapping(source = "idFactura", target = "orderId"),
            @Mapping(source = "fecha", target = "date"),
            @Mapping(source = "subTotalExcento", target = "subTotalExcento"),
            @Mapping(source = "total", target = "total"),
            @Mapping(source = "clienteId", target = "costomerId"),
            @Mapping(source = "estado", target = "active"),
            @Mapping(source = "isvOtros", target = "isvOther"),
            @Mapping(source = "vendedorCod", target = "sellerId"),
            @Mapping(source = "cliente", target = "costomer"),
            @Mapping(source = "usuario", target = "user"),
            @Mapping(source = "observacion", target = "obser"),
            @Mapping(source = "detalles", target = "details")

    })
    Order toOrder(Orden orden);

    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "vendedor", ignore = true),
            @Mapping(target = "cliente", ignore = true)

    })
    Orden toOrden(Order order);

    List<Order> toOrders(List<Orden> ordenes);

}

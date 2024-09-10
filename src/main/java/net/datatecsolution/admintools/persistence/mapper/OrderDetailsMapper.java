package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.OrderDetails;
import net.datatecsolution.admintools.persistence.entity.DetalleFactura;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductMapper.class,OrderMapper.class})
public interface OrderDetailsMapper {
    @Mappings({

            @Mapping(source = "idDetalle", target = "detailId"),
            @Mapping(source = "cantidad", target = "amount"),
            @Mapping(source = "impuesto", target = "tax"),
            @Mapping(source = "total", target = "total"),
            @Mapping(source = "subTotal", target = "subTotal"),
            @Mapping(source = "detallePrecioId", target = "priceItemId"),
            @Mapping(source = "descuentoItem", target = "discountItem"),
            @Mapping(source = "precioVentaItem", target = "priceItem"),
            @Mapping(source = "codigoArt", target = "productId"),
            @Mapping(source = "descuento", target = "discount"),
            @Mapping(source = "idFactura", target = "orderId"),
            @Mapping(source = "articulo", target = "product")


    })
    OrderDetails toOrderDetails(DetalleFactura detalleFactura);

    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "art", ignore = true),
            @Mapping(target = "idDetalle", ignore = true),
            @Mapping(target = "totalVentasCosto", ignore = true),
            @Mapping(target = "ganancia", ignore = true),
            @Mapping(target = "accion", ignore = true),
            @Mapping(target = "factura", ignore = true)

    })
    DetalleFactura toDetalleFactura(OrderDetails orderDetails);

    List<DetalleFactura> toDetalleFacturas(List<OrderDetails> orderDetails);
}

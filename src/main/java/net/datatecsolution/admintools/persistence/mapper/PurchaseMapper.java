package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.PurchaseLineResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseResponse;
import net.datatecsolution.admintools.persistence.entity.DetalleFacturaCompra;
import net.datatecsolution.admintools.persistence.entity.EncabezadoFacturaCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    @Mappings({
            @Mapping(source = "numeroCompra",                   target = "id"),
            @Mapping(source = "codigoProveedor",                target = "supplierId"),
            @Mapping(source = "proveedor.nombreProveedor",      target = "supplierName"),
            @Mapping(source = "noFacturaCompra",                target = "supplierInvoiceNumber"),
            @Mapping(source = "codigoBodega",                   target = "warehouseCode"),
            @Mapping(source = "fecha",                          target = "date"),
            @Mapping(source = "estadoFactura",                  target = "status"),
            @Mapping(source = "tipoFactura",                    target = "invoiceType"),
            @Mapping(source = "fechaVencimiento",               target = "dueDate"),
            @Mapping(source = "subtotal",                       target = "subtotal"),
            @Mapping(source = "impuesto",                       target = "tax"),
            @Mapping(source = "total",                          target = "total"),
            @Mapping(source = "pago",                           target = "payment"),
            @Mapping(source = "usuario",                        target = "user"),
            @Mapping(source = "lineas",                         target = "lines")
    })
    PurchaseResponse toResponse(EncabezadoFacturaCompra encabezado);

    @Mappings({
            @Mapping(source = "idDetalleCompra", target = "id"),
            @Mapping(source = "codigoArticulo",  target = "productId"),
            @Mapping(target = "productName", ignore = true),
            @Mapping(source = "cantidad",        target = "quantity"),
            @Mapping(source = "precio",          target = "price"),
            @Mapping(source = "impuesto",        target = "tax"),
            @Mapping(source = "subtotal",        target = "subtotal"),
            @Mapping(source = "fechaVenc",       target = "expirationDate")
    })
    PurchaseLineResponse toLineResponse(DetalleFacturaCompra linea);

    List<PurchaseLineResponse> toLineResponses(List<DetalleFacturaCompra> lineas);
}

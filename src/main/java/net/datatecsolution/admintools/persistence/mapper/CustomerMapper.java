package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Customer;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * Mapper Cliente (entity) <-> Customer (domain POJO) <-> CustomerResponse/Request (DTOs).
 * Reemplaza el antiguo {@code CostomerMapper} (typo historico).
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mappings({
            @Mapping(source = "id",        target = "customerId"),
            @Mapping(source = "nombre",    target = "customerName"),
            @Mapping(source = "direccion", target = "customerAdress"),
            @Mapping(source = "telefono",  target = "customerTelephoneNumber"),
            @Mapping(source = "celular",   target = "mobile"),
            @Mapping(source = "rtn",       target = "customerRTN")
            // limiteCredito, idVendedor, tipoCliente: auto (mismo nombre).
            // vendedorNombre y saldo no salen de la entidad: los llena el service.
    })
    Customer toCustomer(Cliente cliente);

    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "vendedor",   ignore = true),
            @Mapping(target = "idVendedor", ignore = true)
    })
    Cliente toCliente(Customer customer);

    List<Customer> toCustomers(List<Cliente> clientes);

    // US-019: domain POJO -> DTO de salida (contrato JSON limpio).
    @Mappings({
            @Mapping(source = "customerId",              target = "id"),
            @Mapping(source = "customerName",            target = "name"),
            @Mapping(source = "customerRTN",             target = "rtn"),
            @Mapping(source = "customerAdress",          target = "address"),
            @Mapping(source = "customerTelephoneNumber", target = "phone")
    })
    CustomerResponse toResponse(Customer customer);

    // US-019: DTO de entrada -> domain POJO (id lo asigna la BD).
    @Mappings({
            @Mapping(target = "customerId",              ignore = true),
            @Mapping(source = "tipoCliente",             target = "tipoCliente"), // null -> el repo usa 2 (legacy)
            @Mapping(source = "name",                    target = "customerName"),
            @Mapping(source = "rtn",                     target = "customerRTN"),
            @Mapping(source = "address",                 target = "customerAdress"),
            @Mapping(source = "phone",                   target = "customerTelephoneNumber")
    })
    Customer fromCreateRequest(CustomerCreateRequest request);
}

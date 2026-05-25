package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CostomerMapper {
    @Mappings({
            @Mapping(source = "id", target = "costomerId"),
            @Mapping(source = "nombre", target = "costomerName"),
            @Mapping(source = "direccion", target = "costomerAdress"),
            @Mapping(source = "telefono", target = "costomerTelephoneNumber"),
            @Mapping(source = "rtn", target = "costomerRTN")

    })
    Costomer toCostomer(Cliente cliente);

    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "vendedor", ignore = true),
            @Mapping(target = "idVendedor", ignore = true)

    })
    Cliente toCliente(Costomer costomer);

    List<Costomer> toCostomers(List<Cliente> clientes);

    // US-019: mapeo POJO de dominio -> DTO de salida (contrato JSON limpio).
    @Mappings({
            @Mapping(source = "costomerId", target = "id"),
            @Mapping(source = "costomerName", target = "name"),
            @Mapping(source = "costomerRTN", target = "rtn"),
            @Mapping(source = "costomerAdress", target = "address"),
            @Mapping(source = "costomerTelephoneNumber", target = "phone")
    })
    CustomerResponse toResponse(Costomer costomer);
}

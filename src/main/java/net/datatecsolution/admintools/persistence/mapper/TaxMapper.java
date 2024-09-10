package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Tax;
import net.datatecsolution.admintools.persistence.entity.Impuesto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TaxMapper {
    @Mappings({
            @Mapping(source = "id", target = "taxId"),
            @Mapping(source = "porcentaje", target = "percentage")
    })
    Tax toTax(Impuesto impuesto);

    @InheritInverseConfiguration
    Impuesto toImpuesto(Tax tax);
}

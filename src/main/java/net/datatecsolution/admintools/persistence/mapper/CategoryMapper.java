package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Category;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mappings({
            @Mapping(source = "id", target = "categoryId"),
            @Mapping(source = "descripcion", target = "category"),
    })
    Category toCategory(Categoria categoria);

    @InheritInverseConfiguration
    @Mapping(target = "articulos", ignore = true)
    @Mapping(target = "observacion", ignore = true)
    Categoria toCategoria(Category category);
}


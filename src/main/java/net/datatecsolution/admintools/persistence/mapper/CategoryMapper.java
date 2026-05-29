package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.Category;
import net.datatecsolution.admintools.domain.dto.CategoryRequest;
import net.datatecsolution.admintools.domain.dto.CategoryResponse;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Mapper Categoria (entity) <-> Category (POJO legacy) o
 * CategoryRequest/Response (DTOs Sprint 4 #50).
 *
 * Naming historico:
 *   marcas.codigo_marca (columna) -> Categoria.id (entity)
 *                                  -> Category.categoryId (POJO legacy)
 *                                  -> CategoryResponse.id (DTO Sprint 4)
 *   marcas.descripcion (columna) -> Categoria.descripcion (entity)
 *                                  -> Category.category (POJO legacy)
 *                                  -> CategoryResponse.name (DTO Sprint 4)
 *   marcas.observacion (columna) -> Categoria.observacion (entity)
 *                                  -> CategoryResponse.description (DTO Sprint 4)
 *
 * Los metodos legacy (toCategory/toCategoria) se preservan para no
 * romper consumidores Sprint 1/2 que usen el POJO Category.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // ---- Legacy (Category POJO, Sprint 1/2) ----

    @Mappings({
            @Mapping(source = "id",          target = "categoryId"),
            @Mapping(source = "descripcion", target = "category"),
    })
    Category toCategory(Categoria categoria);

    @InheritInverseConfiguration
    @Mapping(target = "articulos",   ignore = true)
    @Mapping(target = "observacion", ignore = true)
    Categoria toCategoria(Category category);

    // ---- Sprint 4 #50 — DTOs records ----

    @Mappings({
            @Mapping(source = "id",          target = "id"),
            @Mapping(source = "descripcion", target = "name"),
            @Mapping(source = "observacion", target = "description")
    })
    CategoryResponse toResponse(Categoria categoria);

    @Mappings({
            @Mapping(target = "id",          ignore = true),
            @Mapping(target = "articulos",   ignore = true),
            @Mapping(source = "name",        target = "descripcion"),
            @Mapping(source = "description", target = "observacion")
    })
    Categoria fromRequest(CategoryRequest request);
}

package net.datatecsolution.admintools.domain.dto;

/**
 * Sprint 4 #50 — respuesta JSON de una categoria. Keys en ingles segun
 * el contrato establecido en Sprint 3 (CustomerResponse, ProductResponse).
 */
public record CategoryResponse(
        Integer id,
        String name,
        String description,
        Boolean posVisible,

        /** US-081: id de la categoría padre (null = raíz). */
        Integer parentId
) {
}

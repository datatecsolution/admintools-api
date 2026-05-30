package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Sprint 4.5 fix — un precio asignado a un articulo (fila de
 * precios_articulos enriquecida con el nombre del tipo).
 *
 * priceTypeName se devuelve para que el frontend no tenga que hacer
 * lookup contra el catalogo en cada render.
 */
public record ProductPriceResponse(
        Integer id,
        Integer productId,
        Integer priceTypeId,
        String priceTypeName,
        BigDecimal value
) {}

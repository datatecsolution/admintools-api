package net.datatecsolution.admintools.domain.dto;

/**
 * Sprint 4.5 fix — un tipo de precio del catalogo (tabla precios).
 *
 * Tipos iniciales en BD (codigo_precio → descripcion):
 *   1 → "Publico General"   (precio de venta principal)
 *   2 → "Clientes Especiales"
 *   3 → "Mayoristas"
 *   4 → "Costos"
 *
 * El catalogo es extensible: insertar una fila en `precios` agrega un
 * tipo nuevo y el modal de producto del frontend lo renderiza
 * automaticamente (1 input por fila del catalogo).
 *
 * Usado por el frontend de productos para renderizar el listado de
 * precios de cada articulo con su nombre legible (no "codigo 1, 2, 3").
 */
public record PriceCatalogResponse(
        Integer id,
        String name
) {}

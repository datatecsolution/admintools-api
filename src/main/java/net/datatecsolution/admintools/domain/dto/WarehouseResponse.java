package net.datatecsolution.admintools.domain.dto;

/**
 * DTO de salida para una bodega (INV-3). El espejo en departamento es
 * implementacion interna, no se expone.
 */
public record WarehouseResponse(
        Integer code,
        String description
) {
}

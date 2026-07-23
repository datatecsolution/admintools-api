package net.datatecsolution.admintools.domain.dto;

/**
 * Sprint 4.5 fix — una asignacion caja ↔ usuario.
 *
 * cajaName se denormaliza desde `cajas.descripcion` para evitar que
 * el frontend tenga que joinear contra el catalogo.
 *
 * US-105 pulido: warehouseId (cajas.codigo_bodega) — el selector manual
 * del POS solo bloquea el cambio de caja con carrito cuando las cajas
 * asignadas descuentan de BODEGAS DISTINTAS; con la misma bodega el
 * stock es el mismo y el cambio es inocuo. Campo aditivo.
 */
public record UserCajaResponse(
        Integer cajaId,
        String cajaName,
        boolean isDefault,
        Integer warehouseId
) {}

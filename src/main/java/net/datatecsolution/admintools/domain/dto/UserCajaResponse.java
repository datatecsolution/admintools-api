package net.datatecsolution.admintools.domain.dto;

/**
 * Sprint 4.5 fix — una asignacion caja ↔ usuario.
 *
 * cajaName se denormaliza desde `cajas.descripcion` para evitar que
 * el frontend tenga que joinear contra el catalogo.
 */
public record UserCajaResponse(
        Integer cajaId,
        String cajaName,
        boolean isDefault
) {}

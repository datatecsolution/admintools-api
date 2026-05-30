package net.datatecsolution.admintools.domain.dto;

/**
 * Sprint 4.5 fix — caja del catalogo (tabla cajas).
 *
 * nombreDb es el schema MySQL del cliente para esa caja
 * (admin_tools_caja_1, etc.); util si en algun momento se quiere
 * inspeccionar la conexion, pero opcional desde el panel.
 */
public record CajaResponse(
        Integer id,
        String name,
        Integer warehouseId,
        String dbName
) {}

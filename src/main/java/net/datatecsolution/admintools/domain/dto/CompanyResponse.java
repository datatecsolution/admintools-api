package net.datatecsolution.admintools.domain.dto;

/**
 * US-031 — Datos de la empresa expuestos al panel admin.
 */
public record CompanyResponse(
        Integer id,
        String nombre,
        String rtn,
        String telefono,
        String correo,
        String propietario,
        String direccion,
        String logoUrl
) {
}

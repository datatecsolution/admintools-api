package net.datatecsolution.admintools.domain.dto;

/**
 * DTO de salida para Cliente. Define el contrato JSON que ve el frontend,
 * desacoplado del POJO de dominio y de la entidad JPA.
 */
public record CustomerResponse(
        Integer id,
        String name,
        String rtn,
        String address,
        String phone,
        /** 1 = contado/rápido; 2 = gestionado (elegible a crédito). */
        Integer tipoCliente
) {
}

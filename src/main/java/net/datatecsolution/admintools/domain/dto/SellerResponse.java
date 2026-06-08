package net.datatecsolution.admintools.domain.dto;

/** Un vendedor (empleado) para el selector del POS. */
public record SellerResponse(
        Integer id,
        String nombre
) {
}

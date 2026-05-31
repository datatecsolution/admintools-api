package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * Config de vendedor para el checkout del POS: si el usuario tiene
 * {@code ventana_vendedor=1} debe elegir vendedor; sino se usa el default (1).
 */
public record SellerSettingsResponse(
        boolean ventanaVendedor,
        List<SellerResponse> sellers
) {
}

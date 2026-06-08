package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * Config del checkout del POS (de config_user_facturacion): si
 * {@code ventana_vendedor=1} debe elegir vendedor (sino default 1); si
 * {@code ventana_observaciones=1} se pide observación de la factura; si
 * {@code descuento_porcentaje=1} el descuento por línea se ingresa en %
 * (sino en monto/L).
 */
public record SellerSettingsResponse(
        boolean ventanaVendedor,
        boolean ventanaObservaciones,
        boolean descuentoEnPorcentaje,
        List<SellerResponse> sellers
) {
}

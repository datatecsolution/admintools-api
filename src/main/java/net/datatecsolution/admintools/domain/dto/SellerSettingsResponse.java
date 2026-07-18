package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * Config del checkout del POS (de config_user_facturacion): si
 * {@code ventana_vendedor=1} debe elegir vendedor (sino default 1); si
 * {@code ventana_observaciones=1} se pide observación de la factura; si
 * {@code descuento_porcentaje=1} el descuento por línea se ingresa en %
 * (sino en monto/L); si {@code pwd_precio=1} / {@code pwd_descuento=1} la acción
 * de Precios / Descuentos pide contraseña de administrador.
 */
public record SellerSettingsResponse(
        boolean ventanaVendedor,
        boolean ventanaObservaciones,
        boolean descuentoEnPorcentaje,
        boolean pwdPrecio,
        boolean pwdDescuento,
        /** true: el cajero puede crear clientes tipo 2 (crédito) desde el POS. */
        boolean crearClienteCredito,
        List<SellerResponse> sellers,
        /**
         * US-105: rotación automática de cajas (US-102) encendida para este
         * usuario — el POS OCULTA el selector manual de caja cuando es true
         * (paridad Swing: Ctrl+P deshabilitado con la automática activa).
         */
        boolean rotacionAutomaticaCajas
) {
}

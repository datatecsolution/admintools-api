package net.datatecsolution.admintools.domain.dto;

/**
 * US-031 — Parametros generales de facturacion (config_app).
 */
public record ConfigResponse(
        Integer diaVencimientoFactura,
        Integer interesParaFacturasVenc
) {
}

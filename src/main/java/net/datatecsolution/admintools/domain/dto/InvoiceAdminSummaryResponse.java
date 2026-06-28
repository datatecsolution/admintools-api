package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * Total server-side de la sección Facturas (US-099): cantidad de facturas y
 * suma de totales (excluyendo anuladas) para el filtro actual — calculado en
 * SQL sobre todas las cajas, sin traer las filas. Evita el descuadre del total
 * que se sumaba en cliente sobre una página capada.
 */
public record InvoiceAdminSummaryResponse(
        long count,
        BigDecimal total
) {
}

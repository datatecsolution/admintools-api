package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;

/**
 * US-040 — Bloque fiscal SAR de una factura, para imprimir el ticket fiscal.
 * Se deriva del rango autorizado de la caja ({@code datos_factura}, enlazado vía
 * {@code encabezado_factura.cod_rango}) más el {@code numero_factura}. Espejo de
 * lo que imprime el Swing (jrxml {@code factura_carta} / {@code factura_datos_dei}):
 * número fiscal con prefijo, CAI, rango autorizado y fecha límite de emisión.
 *
 * {@code restantes} = facturas que quedan en el rango ({@code facturaFinal − numero}),
 * para que el POS avise (no bloquea) cuando el CAI está por agotarse; {@code null}
 * si el rango no es numérico. Degrada con seguridad si no hay rango configurado:
 * {@code cai="NA"} y prefijo vacío.
 */
public record FiscalInfo(
        String numeroFiscal,
        String cai,
        String rangoAutorizado,
        LocalDate fechaLimiteEmision,
        Integer restantes,
        /** Sucursal del rango (datos_factura.observacion); null si vacía. */
        String sucursal
) {

    /**
     * Construye el bloque fiscal a partir de los valores crudos de
     * {@code datos_factura} y el número de factura. Tolera prefijo/rango
     * ausentes o no numéricos ('NA').
     */
    public static FiscalInfo build(String codigoTipoFacturacion, String cai,
                                   String facturaInicial, String facturaFinal,
                                   LocalDate fechaLimiteEmision, String observacion,
                                   int numeroFactura) {
        String pref = codigoTipoFacturacion == null ? "" : codigoTipoFacturacion.trim();
        if ("NA".equalsIgnoreCase(pref)) pref = "";
        String caiSafe = (cai == null || cai.isBlank()) ? "NA" : cai.trim();

        String numeroFiscal = withPrefix(pref, numeroFactura);

        Integer ini = parseIntOrNull(facturaInicial);
        Integer fin = parseIntOrNull(facturaFinal);
        String rango = (ini != null && fin != null)
                ? withPrefix(pref, ini) + " al " + withPrefix(pref, fin)
                : null;
        Integer restantes = fin != null ? (fin - numeroFactura) : null;

        String sucursal = (observacion == null || observacion.isBlank()
                || "NA".equalsIgnoreCase(observacion.trim())) ? null : observacion.trim();

        return new FiscalInfo(numeroFiscal, caiSafe, rango, fechaLimiteEmision, restantes, sucursal);
    }

    private static String withPrefix(String pref, int numero) {
        String num = String.format("%08d", numero);
        return pref.isEmpty() ? num : pref + "-" + num;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

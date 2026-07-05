package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;

/**
 * US-101 — fila de datos_factura de una caja (CAI/rango fiscal).
 * enUso = ya hay facturas emitidas con este cod_rango → no se puede borrar
 * (la UI puede deshabilitar la accion sin esperar el 409).
 * usadas = numeros CONSUMIDOS del rango (clamp del ultimo numero_factura de
 * la caja contra [inicial, final]); alimenta el medidor de consumo y el
 * "proximo numero" (inicial + usadas) de la pantalla de Cajas.
 */
public record FiscalRangeResponse(
        Integer id,
        String cai,
        Integer facturaInicial,
        Integer facturaFinal,
        String codigoTipoFacturacion,
        Integer cantidadSolicitada,
        LocalDate fechaLimiteEmision,
        String observacion,
        long usadas,
        boolean enUso
) {}

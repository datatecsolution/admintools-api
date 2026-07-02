package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;

/**
 * US-101 — fila de datos_factura de una caja (CAI/rango fiscal).
 * enUso = ya hay facturas emitidas con este cod_rango → no se puede borrar
 * (la UI puede deshabilitar la accion sin esperar el 409).
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
        boolean enUso
) {}

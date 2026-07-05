package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * US-101 — rangos fiscales de una caja + su último número emitido.
 * ultimoNumero permite a la UI explicar el escenario de sobrepaso: si el
 * cliente ya facturó por encima del rango nuevo autorizado, las emitidas
 * conservan su rango y la numeración CONTINÚA (no se reinicia en inicial).
 */
public record FiscalRangesResponse(
        int ultimoNumero,
        List<FiscalRangeResponse> ranges
) {}

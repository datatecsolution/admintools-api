package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * US-108 — Re-lectura de un cierre de caja para reimprimir su comprobante.
 * Son los numeros que {@code cerrar()} dejo persistidos en cierre_caja (no un
 * recalculo), mas la lista de salidas del turno por el rango guardado
 * [no_salida_inicial, no_salida_final] — como el Jasper cierre_caja + su
 * subreporte cierre_salida. {@code caja} sigue la convencion del resumen:
 * nombre de la caja, o "N cajas" si el turno fue multi-caja.
 */
public record CierreDetalleResponse(
        Integer id,
        String caja,
        String usuario,
        String turno,
        Integer estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinal,
        BigDecimal apertura,
        BigDecimal ventaEfectivo,
        BigDecimal totalCobro,
        BigDecimal totalEntrada,
        BigDecimal totalSalida,
        BigDecimal totalPago,
        BigDecimal totalEfectivo,
        BigDecimal efectivoContado,
        BigDecimal totalExcento,
        BigDecimal isv15,
        BigDecimal isv18,
        BigDecimal ventaTarjeta,
        BigDecimal ventaCredito,
        BigDecimal totalVenta,
        List<CierreResumenResponse.SalidaTurno> salidas
) {
}

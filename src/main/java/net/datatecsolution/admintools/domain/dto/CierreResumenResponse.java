package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen del turno para el cuadre del cierre (shape de {@code CierreResumen}
 * del POS). Son los mismos numeros que {@code CierreCajaDao.actualizarCierre}
 * del Swing calcula al cerrar, pero en read-only para previsualizar:
 *
 *   efectivo esperado = apertura + ventaEfectivo + totalCobro + totalEntrada
 *                       - totalSalida - totalPago
 *
 * totalIsv15/totalIsv18 son el impuesto recaudado (columnas isv15/isv18 del
 * cierre); totalExcento es la venta exenta (subtotal_excento).
 * noFacturaInicio/Final es el rango de la caja de la sesion (tenant del JWT).
 *
 * US-103: {@code cajas} es el desglose por caja del turno (una fila por
 * cierre_facturacion, con su rango de folios y sus ventas). Los escalares
 * consolidados y el rango de la caja de la sesion se conservan por compat
 * con el POS viejo; con varias cajas {@code caja} pasa a "N cajas".
 *
 * US-108: {@code salidas} lista las salidas de caja del turno (subreporte
 * cierre_salida del Swing) para la seccion SALIDAS DEL TURNO del comprobante;
 * mismo rango y filtro (estado ACT) que el escalar {@code totalSalida}.
 */
public record CierreResumenResponse(
        String caja,
        String usuario,
        String turno,
        String fecha,
        BigDecimal apertura,
        BigDecimal ventaEfectivo,
        BigDecimal ventaTarjeta,
        BigDecimal ventaCredito,
        BigDecimal totalExcento,
        BigDecimal totalIsv15,
        BigDecimal totalIsv18,
        BigDecimal totalCobro,
        BigDecimal totalEntrada,
        BigDecimal totalSalida,
        BigDecimal totalPago,
        Integer noFacturaInicio,
        Integer noFacturaFinal,
        List<CajaResumen> cajas,
        List<SalidaTurno> salidas
) {
    /** US-103 — una caja del turno: rango de folios + ventas del cuadre. */
    public record CajaResumen(
            Integer codigoCaja,
            String caja,
            Integer noFacturaInicio,
            Integer noFacturaFinal,
            Integer numVentas,
            BigDecimal totalVentas
    ) {}

    /** US-108 — una salida de caja del turno (fila del subreporte cierre_salida). */
    public record SalidaTurno(
            Integer numero,
            LocalDateTime fecha,
            String concepto,
            BigDecimal monto
    ) {}
}

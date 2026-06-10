package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

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
        Integer noFacturaFinal
) {}

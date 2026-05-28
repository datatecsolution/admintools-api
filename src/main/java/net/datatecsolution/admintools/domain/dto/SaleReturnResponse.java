package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de una devolucion (o agrupacion por factura). Cuando viene
 * de POST /sale-returns, agrupa todos los items recien creados.
 * Cuando viene de GET /sale-returns/by-invoice/{n}, agrupa todo el
 * historial de devoluciones de esa factura.
 */
public record SaleReturnResponse(
        Integer invoiceNumber,
        Integer cajaCode,
        String tenant,
        String user,
        LocalDate date,
        BigDecimal totalReturned,
        List<SaleReturnLineResponse> items
) {
}

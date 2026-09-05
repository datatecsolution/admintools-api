package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * US-064 — respuesta de GET /reports/purchase-suggestions. El período base
 * (default últimos 30 días) define la venta diaria promedio; horizonteDias
 * es cuántos días de venta debe cubrir la compra sugerida.
 */
public record PurchaseSuggestionsResponse(
        LocalDate from,
        LocalDate to,
        Integer caja,
        Integer bodega,
        long dias,
        int horizonteDias,
        List<PurchaseUrgencySummary> resumen,
        List<PurchaseSuggestionItem> items
) {
}

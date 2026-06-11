package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * Ranking de ventas para ordenar el catálogo de facturación (US-094):
 * categorías ordenadas por unidades vendidas en la ventana configurada y el
 * top de productos más vendidos (para el chip "Más vendidos"). Listas vacías
 * si la caja no tiene ventas en la ventana (degradación limpia en el POS).
 */
public record SalesRankingResponse(
        List<Integer> categoryIdsByRank,
        List<Integer> topProductIds,
        int days
) {
}

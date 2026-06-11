package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * Ranking de ventas para ordenar el catálogo de facturación (US-094):
 * categorías y productos ordenados por unidades vendidas en la ventana
 * configurada. El POS ordena los chips por {@code categoryIdsByRank} y, dentro
 * de cada categoría, sube los más vendidos según {@code productIdsByRank}.
 * Listas vacías si la caja no tiene ventas en la ventana (degradación limpia).
 */
public record SalesRankingResponse(
        List<Integer> categoryIdsByRank,
        List<Integer> productIdsByRank,
        int days
) {
}

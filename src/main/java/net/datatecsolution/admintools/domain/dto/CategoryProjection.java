package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-106 — proyección de un trimestre del año en curso para una categoría.
 * metodo documenta CÓMO se estimó (la proyección nunca se disfraza de dato):
 *  - FACTOR_YOY:     trimestre del año anterior × (YTD actual / YTD anterior
 *                    al mismo día del año) — el método del análisis de dulce.
 *  - RUN_RATE:       parcial del trimestre en curso × días del trimestre /
 *                    días transcurridos (categorías sin base interanual).
 *  - ANIO_ANTERIOR:  se asume repetir la temporada del año pasado (trimestres
 *                    futuros sin factor).
 */
public record CategoryProjection(
        int anio,
        int trimestre,
        Integer codigoCategoria,
        String categoria,
        BigDecimal unidades,
        BigDecimal venta,
        String metodo
) {
}

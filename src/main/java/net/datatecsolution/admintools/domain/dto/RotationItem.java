package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;

/**
 * US-063 — una fila del reporte de rotación: ventas del período, stock
 * actual (saldo materializado, US-131/132) y las métricas derivadas.
 * diasCobertura es null cuando no hubo venta (cobertura infinita);
 * rotacion (unidades vendidas / stock actual) es null cuando no hay stock.
 */
public record RotationItem(
        int codigoArticulo,
        String articulo,
        Integer codigoCategoria,
        String categoria,
        BigDecimal unidades,
        BigDecimal venta,
        BigDecimal stockActual,
        BigDecimal ventaDiaria,
        BigDecimal diasCobertura,
        BigDecimal rotacion,
        boolean sinMovimiento,
        String clasificacion
) {
}

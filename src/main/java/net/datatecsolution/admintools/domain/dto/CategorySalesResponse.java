package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * US-106 — respuesta de GET /reports/category-sales: el comparativo
 * trimestral crudo (filas YEAR×QUARTER×categoría, como el SQL validado de
 * dulce) + las proyecciones de los trimestres restantes del año en curso.
 * hasta = fecha de corte de los datos reales (hoy en la zona del negocio).
 */
public record CategorySalesResponse(
        int fromYear,
        LocalDate hasta,
        Integer caja,
        List<CategorySalesRow> filas,
        List<CategoryProjection> proyecciones
) {
}

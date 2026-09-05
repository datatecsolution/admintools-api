package net.datatecsolution.admintools.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * US-063 — respuesta de GET /reports/rotation. caja=null consolida todas
 * las cajas; bodega=null suma el stock de todas las bodegas. Los umbrales
 * son días de cobertura: RAPIDO ≤ umbralRapidoDias, MEDIO ≤ umbralMedioDias,
 * LENTO el resto (incluye sin movimiento).
 */
public record RotationReportResponse(
        LocalDate from,
        LocalDate to,
        Integer caja,
        Integer bodega,
        long dias,
        int umbralRapidoDias,
        int umbralMedioDias,
        List<RotationClassSummary> resumen,
        List<RotationItem> items
) {
}

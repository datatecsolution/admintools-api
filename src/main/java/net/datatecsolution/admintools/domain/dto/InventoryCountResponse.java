package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryCountResponse(
        Integer id,
        LocalDateTime fecha,
        String usuario,
        Integer warehouseCode,
        Integer contadas,
        Integer faltantes,
        Integer sobrantes,
        Integer negativos,
        BigDecimal valorAjuste,
        BigDecimal valorNegativos,
        String motivo,
        String estado,
        List<InventoryCountLineResponse> lines
) {
}

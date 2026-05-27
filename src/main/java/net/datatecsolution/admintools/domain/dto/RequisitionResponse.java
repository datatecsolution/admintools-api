package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RequisitionResponse(
        Integer id,
        Integer originWarehouseCode,
        String originWarehouseName,
        Integer destinationWarehouseCode,
        String destinationWarehouseName,
        LocalDateTime date,
        String status,
        BigDecimal total,
        String user,
        List<RequisitionLineResponse> lines
) {
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseResponse(
        Integer id,
        Integer supplierId,
        String supplierName,
        String supplierInvoiceNumber,
        Integer warehouseCode,
        LocalDate date,
        String status,
        Integer invoiceType,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        BigDecimal payment,
        String user,
        List<PurchaseLineResponse> lines
) {
}

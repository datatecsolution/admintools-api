package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request para crear una compra completa (INV-5). El service calcula
 * totales server-side a partir de las lineas; los inserts en
 * {@code detalle_factura_compra} disparan el kardex automaticamente.
 */
public record PurchaseRequest(
        @NotNull(message = "supplierId es obligatorio")
        @Positive(message = "supplierId debe ser positivo")
        Integer supplierId,

        @NotBlank(message = "supplierInvoiceNumber es obligatorio")
        @Size(max = 100, message = "supplierInvoiceNumber no puede exceder 100 caracteres")
        String supplierInvoiceNumber,

        @NotNull(message = "warehouseCode es obligatorio")
        @Positive(message = "warehouseCode debe ser positivo")
        Integer warehouseCode,

        LocalDate date,         // opcional; default = hoy

        @Size(max = 5, message = "invoiceCode no puede exceder 5 caracteres")
        String invoiceCode,     // opcional; default "NA"

        @NotNull(message = "invoiceType es obligatorio")
        Integer invoiceType,

        LocalDate dueDate,      // opcional; default = date

        @PositiveOrZero(message = "paymentAmount no puede ser negativo")
        BigDecimal paymentAmount, // opcional; default 0

        @NotEmpty(message = "la compra debe tener al menos una linea")
        @Valid
        List<PurchaseLineRequest> lines
) {
}

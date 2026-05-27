package net.datatecsolution.admintools.domain.dto;

/** Read-only DTO de proveedor (INV-5). */
public record SupplierResponse(
        Integer id,
        String name,
        String phone,
        String mobile,
        String address
) {
}

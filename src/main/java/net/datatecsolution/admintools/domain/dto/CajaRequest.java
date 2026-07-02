package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * US-101 — alta/edicion de caja (espejo de ViewCrearCaja del Swing:
 * descripcion + bodega). El nombre_db NO es editable: lo deriva el
 * provisioning (admin_tools_caja_{codigo}), igual que CajaDao.registrar.
 */
public record CajaRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull Integer warehouseId
) {}

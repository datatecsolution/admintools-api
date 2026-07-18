package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * US-104 — body de {@code PUT /users/{id}/rotacion}: activa/desactiva la
 * rotación automática de cajas del usuario (flag
 * {@code config_user_facturacion.rotacion_automatica_cajas}). Encender exige
 * cajero (tipoPermiso 2) con exactamente 2 cajas asignadas.
 */
public record RotacionToggleRequest(@NotNull Boolean enabled) {
}

package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * US-101 — alta/edicion de datos de facturacion (CAI/rango) de una caja.
 * Espejo de ViewCrearDatosFacturacion del Swing: mismas obligatoriedades
 * (CtlDatosFacturacion.validar). observacion es la "sucursal" que imprime
 * el ticket fiscal (FiscalInfo, US-040); el Swing la deja vacia.
 */
public record FiscalRangeRequest(
        @NotBlank @Size(max = 300) String cai,
        @NotNull @Positive Integer facturaInicial,
        @NotNull @Positive Integer facturaFinal,
        @NotBlank @Size(max = 50) String codigoTipoFacturacion,
        @NotNull @Positive Integer cantidadSolicitada,
        @NotNull LocalDate fechaLimiteEmision,
        @Size(max = 255) String observacion
) {}

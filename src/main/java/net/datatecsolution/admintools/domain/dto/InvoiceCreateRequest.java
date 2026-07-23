package net.datatecsolution.admintools.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout POS — crear factura directo desde el carrito (sin orden).
 * {@code tipoFactura}: 1=contado, 2=crédito.
 *
 * Cliente: si {@code customerId} es válido (>0) se factura a ese cliente; si no
 * y viene {@code customerName}, se registra un cliente <b>contado (tipo 1)</b>
 * al vuelo (fiel al Swing {@code registrarClienteContado}) y se factura a él. Si
 * no hay ninguno, cae a Consumidor final (id 1). El <b>crédito exige un cliente
 * gestionado (tipo 2)</b>; contado/escritos (tipo 1) no pueden recibir crédito.
 */
public record InvoiceCreateRequest(
        Integer customerId,
        String customerName,
        /** RTN del cliente escrito (solo dígitos); se guarda al auto-crear el contado. */
        String customerRtn,
        @NotNull Integer tipoFactura,
        Integer tipoPago,
        Integer vendedorId,
        BigDecimal cobroEfectivo,
        BigDecimal cobroTarjeta,
        String observacion,
        /**
         * Si el ticket vino de una orden recuperada: al facturar se marca esa
         * orden como facturada (estado 3) y sale de pendientes — espejo del
         * Swing, que factura lo que está en pantalla y luego remata la orden
         * temporal. Se factura el carrito enviado (puede venir modificado),
         * NO el contenido guardado de la orden.
         */
        Integer orderId,
        @NotEmpty(message = "la factura debe tener al menos una línea")
        @Valid
        List<InvoiceLineRequest> lines,
        /**
         * US-105 — caja MANUAL elegida por el cajero (selector del POS, paridad
         * Ctrl+P del Swing). Debe estar asignada al usuario (cajas_usuarios) o
         * 403. Tiene prioridad sobre la rotación automática (US-102) y NO
         * consume su bandera. null = caja default del tenant (+ rotación
         * automática si aplica).
         */
        Integer cajaId
) {
}

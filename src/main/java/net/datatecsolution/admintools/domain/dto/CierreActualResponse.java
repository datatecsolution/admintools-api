package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado del turno (cierre) actual del usuario. El POS lo consulta al entrar
 * a /facturacion: si {@code abierto=false} debe pedir la apertura (contar el
 * efectivo inicial), igual que {@code setCierre} del Swing.
 *
 * id es null cuando el usuario nunca ha tenido un cierre.
 */
public record CierreActualResponse(
        Integer id,
        boolean abierto,
        BigDecimal efectivoInicial,
        LocalDateTime fechaInicio,
        String usuario
) {}

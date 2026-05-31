package net.datatecsolution.admintools.persistence.crud;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * US-035 — Proyeccion de un movimiento de kardex (entrada/salida/saldo).
 */
public interface KardexMovementView {
    Integer getCodigoMovimiento();
    LocalDate getFecha();
    Integer getTipoMovimiento();
    String getTipoMovimientoDesc();
    String getDescripcion();
    String getDocumento();
    BigDecimal getCantidad();
    BigDecimal getPrecioUnidad();
    BigDecimal getTotal();
}

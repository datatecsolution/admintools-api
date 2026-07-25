package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * US-108 — Un movimiento de caja (entrada o salida) con los datos que lleva
 * su comprobante. Lo devuelven el POST (recien registrado) y el GET de
 * re-lectura para reimpresion, con los mismos campos. El id NO es unico
 * entre tipos (entradas_caja y salidas_caja numeran aparte), por eso el GET
 * exige el tipo.
 *
 * {@code cuenta} (solo entradas) es la cuenta bancaria destino y
 * {@code empleado} (solo salidas) el empleado que recibe — null cuando el
 * movimiento quedo con los defaults legacy (codigo_cuenta=-1 /
 * codigo_empleado=1 "system").
 */
public record CashMovementResponse(
        Integer id,
        String tipo,
        LocalDateTime fecha,
        String concepto,
        BigDecimal monto,
        String usuario,
        String estado,
        CuentaRef cuenta,
        EmpleadoRef empleado
) {
    /** Cuenta bancaria destino de una entrada (entrada_caja.jrxml: banco · no_cuenta). */
    public record CuentaRef(Integer id, String banco, String noCuenta, String tipoCuenta) {}

    /** Empleado de una salida (salida_caja.jrxml: cod | nombre). */
    public record EmpleadoRef(Integer id, String nombre) {}
}

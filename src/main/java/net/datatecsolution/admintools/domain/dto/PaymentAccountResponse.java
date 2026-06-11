package net.datatecsolution.admintools.domain.dto;

/**
 * Forma de pago a proveedores — fila de la tabla {@code bancos}
 * (BancosDao.getCuentas() del Swing): efectivo de caja + cuentas de banco.
 * {@code efectivo=true} para la fila de efectivo (en el cierre el pago ya
 * cuenta en total_pago; no se duplica con una salida de caja).
 */
public record PaymentAccountResponse(
        Integer id,
        String nombre,
        String cuenta,
        String tipo,
        boolean efectivo
) {
}

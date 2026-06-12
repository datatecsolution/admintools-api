package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CuentaFactura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

import java.util.List;

/**
 * US-033 — Lectura de cuentas por factura. Las queries replican las del
 * {@code CuentaFacturaDao} del Swing, reusando las funciones MySQL de la BD
 * comun ({@code f_saldo_factura_cliente}, {@code f_no_dias_del_ultimo_pago},
 * {@code f_fecha_ultimo_pago_factura}) para mantener saldos identicos.
 *
 * Las columnas se aliasan a los nombres de las propiedades de la proyeccion
 * para que Spring Data las mapee desde la query nativa.
 */
public interface CuentaFacturaCRUD extends JpaRepository<CuentaFactura, Integer> {

    /**
     * Estado por factura de un cliente: cuentas con saldo distinto de 0.
     * Mirror de {@code buscarPorIdCliente}.
     */
    @Query(value = "SELECT cf.codigo_cuenta AS codigoCuenta, " +
            " cf.no_factura AS noFactura, " +
            " cf.codigo_caja AS codigoCaja, " +
            " cf.fecha AS fecha, " +
            " cf.fecha_vencimiento AS fechaVencimiento, " +
            " IFNULL(f_saldo_factura_cliente(cf.codigo_cuenta), 0.00) AS saldo, " +
            " IFNULL(f_no_dias_del_ultimo_pago(cf.codigo_cuenta), 0) AS noDias " +
            " FROM cuentas_facturas cf " +
            " WHERE cf.codigo_cliente = :customerId " +
            "   AND IFNULL(f_saldo_factura_cliente(cf.codigo_cuenta), 0.00) <> 0 " +
            " ORDER BY cf.codigo_cuenta ASC", nativeQuery = true)
    List<InvoiceAccountView> findInvoiceAccountsByCustomer(@Param("customerId") int customerId);

    /**
     * Reporte de morosidad: facturas con saldo pendiente y al menos
     * {@code minDays} dias desde el ultimo movimiento. Mirror de
     * {@code buscarConSaldoReporte}; usa LEFT JOIN a empleados para no
     * descartar facturas de clientes sin cobrador asignado.
     */
    @Query(value = "SELECT cf.codigo_cuenta AS codigoCuenta, " +
            " cf.no_factura AS noFactura, " +
            " cf.codigo_caja AS codigoCaja, " +
            " cf.codigo_cliente AS codigoCliente, " +
            " c.nombre_cliente AS nombreCliente, " +
            " c.telefono AS telefono, " +
            " cf.fecha AS fecha, " +
            " cf.fecha_vencimiento AS fechaVencimiento, " +
            " IFNULL(f_saldo_factura_cliente(cf.codigo_cuenta), 0.00) AS saldo, " +
            " IFNULL(f_no_dias_del_ultimo_pago(cf.codigo_cuenta), 0) AS noDias, " +
            " f_fecha_ultimo_pago_factura(cf.codigo_cuenta) AS ultimoPago, " +
            " TRIM(CONCAT(IFNULL(e.nombre,''),' ',IFNULL(e.apellido,''))) AS nombreVendedor " +
            " FROM cuentas_facturas cf " +
            " JOIN cliente c ON c.codigo_cliente = cf.codigo_cliente " +
            " LEFT JOIN empleados e ON c.id_cobrador = e.codigo_empleado " +
            " WHERE IFNULL(f_saldo_factura_cliente(cf.codigo_cuenta), 0.00) > 0 " +
            "   AND IFNULL(f_no_dias_del_ultimo_pago(cf.codigo_cuenta), 0) >= :minDays " +
            " ORDER BY noDias DESC",
            countQuery = "SELECT COUNT(*) FROM cuentas_facturas cf " +
            " WHERE IFNULL(f_saldo_factura_cliente(cf.codigo_cuenta), 0.00) > 0 " +
            "   AND IFNULL(f_no_dias_del_ultimo_pago(cf.codigo_cuenta), 0) >= :minDays",
            nativeQuery = true)
    Page<DelinquentInvoiceView> findDelinquent(@Param("minDays") int minDays, Pageable pageable);

    /** US-041 — cuenta de una factura (para reversar CxC al anular). */
    Optional<CuentaFactura> findFirstByNoFacturaAndCodigoCaja(Integer noFactura, Integer codigoCaja);

    /** Saldo actual de la factura en CxC (f_saldo_factura_cliente). */
    @Query(value = "SELECT IFNULL(f_saldo_factura_cliente(:codigoCuenta), 0.00)", nativeQuery = true)
    BigDecimal saldoFactura(@Param("codigoCuenta") int codigoCuenta);
}

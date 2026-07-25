package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.PaymentAccountResponse;
import net.datatecsolution.admintools.domain.dto.SupplierBalanceResponse;
import net.datatecsolution.admintools.domain.dto.SupplierPaymentRequest;
import net.datatecsolution.admintools.domain.dto.SupplierReceiptDetailResponse;
import net.datatecsolution.admintools.domain.dto.SupplierReceiptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * Cuenta por pagar (pago a proveedores) — réplica de CtlPagoProveedor +
 * ReciboPagoProveedoresDao + CuentaPorPagarDao del Swing. Todas las tablas
 * viven en la BD comun; se usa JdbcTemplate como en el cierre de caja.
 *
 * Saldo del proveedor: {@code f_saldo_proveedor} (= ultimo cuentas_por_pagar.saldo,
 * 0 si no hay). Un pago: inserta recibo_pago_proveedores (saldo_anterio, saldo =
 * anterior - total, codigo_tipo_pago = banco.id) y un movimiento en
 * cuentas_por_pagar (debito = total, saldo = anterior - total, igual que el
 * Swing). El cierre ya agrega recibo_pago_proveedores en total_pago, asi que NO
 * se crea una salida de caja adicional (seria doble conteo).
 */
@Service
public class SupplierPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SupplierPaymentService.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public SupplierPaymentService(@Qualifier("commonDataSource") DataSource commonDS,
                                  @Qualifier("transactionManager") PlatformTransactionManager commonTm) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.tx = new TransactionTemplate(commonTm);
    }

    /** Proveedores con saldo > 0 (cuenta por pagar pendiente). */
    public List<SupplierBalanceResponse> withBalance() {
        return jdbc.query(
                "SELECT p.codigo_proveedor AS id, p.nombre_proveedor AS name, "
                        + "ifnull(f_saldo_proveedor(p.codigo_proveedor),0) AS saldo "
                        + "FROM proveedor p "
                        + "HAVING saldo > 0 ORDER BY saldo DESC",
                (rs, i) -> new SupplierBalanceResponse(
                        rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("saldo")));
    }

    /** Formas de pago (tabla bancos): efectivo de caja + cuentas de banco. */
    public List<PaymentAccountResponse> paymentAccounts() {
        return jdbc.query(
                "SELECT b.id, b.nombre, b.no_cuenta, tc.tipo_cuenta "
                        + "FROM bancos b LEFT JOIN tipo_cuenta_bancos tc ON b.id_tipo_cuenta = tc.id "
                        + "ORDER BY b.id",
                (rs, i) -> {
                    String nombre = rs.getString("nombre");
                    String cuenta = rs.getString("no_cuenta");
                    boolean efectivo = nombre != null && nombre.trim().equalsIgnoreCase("Efectivo");
                    return new PaymentAccountResponse(
                            rs.getInt("id"), nombre,
                            cuenta == null || cuenta.equalsIgnoreCase("NA") ? null : cuenta,
                            rs.getString("tipo_cuenta"),
                            efectivo);
                });
    }

    /** Registra un pago al proveedor (recibo + débito en CxP). */
    public SupplierReceiptResponse pay(int supplierId, SupplierPaymentRequest req, String user) {
        return tx.execute(status -> {
            String nombre = jdbc.query(
                    "SELECT nombre_proveedor FROM proveedor WHERE codigo_proveedor = ?",
                    rs -> rs.next() ? rs.getString(1) : null, supplierId);
            if (nombre == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado");
            }
            String formaPago = jdbc.query(
                    "SELECT nombre FROM bancos WHERE id = ?",
                    rs -> rs.next() ? rs.getString(1) : null, req.formaPagoId());
            if (formaPago == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Forma de pago inválida");
            }

            BigDecimal saldoAnterior = scale(jdbc.queryForObject(
                    "SELECT ifnull(f_saldo_proveedor(?),0)", BigDecimal.class, supplierId));
            BigDecimal monto = scale(req.monto());
            if (monto.compareTo(saldoAnterior) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El monto excede el saldo del proveedor (" + saldoAnterior + ")");
            }
            BigDecimal nuevoSaldo = scale(saldoAnterior.subtract(monto));
            String concepto = req.concepto() == null || req.concepto().isBlank()
                    ? "Pago a proveedor" : req.concepto().trim();

            // 1) recibo_pago_proveedores
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO recibo_pago_proveedores "
                                + "(fecha, codigo_proveedor, total_letras, total, concepto, usuario, "
                                + "saldo_anterio, saldo, codigo_tipo_pago) "
                                + "VALUES (now(), ?, 'NA', ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, supplierId);
                ps.setBigDecimal(2, monto);
                ps.setString(3, concepto);
                ps.setString(4, user);
                ps.setBigDecimal(5, saldoAnterior);
                ps.setBigDecimal(6, nuevoSaldo);
                ps.setInt(7, req.formaPagoId());
                return ps;
            }, kh);
            int noRecibo = kh.getKey() != null ? kh.getKey().intValue() : 0;

            // 2) movimiento en cuentas_por_pagar (debito = total, saldo reducido)
            //    igual que CuentaPorPagarDao.reguistrarDebito del Swing.
            jdbc.update(
                    "INSERT INTO cuentas_por_pagar (fecha, codigo_proveedor, descripcion, debito, saldo) "
                            + "VALUES (now(), ?, ?, ?, ?)",
                    supplierId, concepto + " con recibo no. " + noRecibo, monto, nuevoSaldo);

            log.info("CxP: pago proveedor {} recibo {} forma {} monto {} saldo {}->{}",
                    supplierId, noRecibo, req.formaPagoId(), monto, saldoAnterior, nuevoSaldo);
            return new SupplierReceiptResponse(noRecibo, supplierId, monto, formaPago, concepto,
                    saldoAnterior, nuevoSaldo);
        });
    }

    /**
     * US-108 — re-lectura de un recibo por numero para reimprimir su
     * comprobante (mirror del Jasper pago_caja / v_recibo_pago_proveedor,
     * pero con LEFT JOIN: la vista usa INNER y pierde recibos con proveedor
     * o forma de pago por defecto -1).
     */
    public SupplierReceiptDetailResponse getReceipt(int noRecibo) {
        List<SupplierReceiptDetailResponse> rows = jdbc.query(
                "SELECT r.no_recibo, r.fecha, r.codigo_proveedor, p.nombre_proveedor, "
                        + "r.total, b.nombre AS forma_pago, r.concepto, r.usuario, "
                        + "r.saldo_anterio, r.saldo "
                        + "FROM recibo_pago_proveedores r "
                        + "LEFT JOIN proveedor p ON r.codigo_proveedor = p.codigo_proveedor "
                        + "LEFT JOIN bancos b ON r.codigo_tipo_pago = b.id "
                        + "WHERE r.no_recibo = ?",
                (rs, i) -> new SupplierReceiptDetailResponse(
                        rs.getInt("no_recibo"),
                        rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                        rs.getInt("codigo_proveedor"),
                        rs.getString("nombre_proveedor") != null ? rs.getString("nombre_proveedor") : "NA",
                        rs.getBigDecimal("total"),
                        rs.getString("forma_pago"),
                        rs.getString("concepto"),
                        rs.getString("usuario"),
                        rs.getBigDecimal("saldo_anterio"),
                        rs.getBigDecimal("saldo")),
                noRecibo);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recibo de proveedor no encontrado.");
        }
        return rows.get(0);
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
    }
}

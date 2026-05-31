package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.AbonoRequest;
import net.datatecsolution.admintools.domain.dto.BalanceResponse;
import net.datatecsolution.admintools.domain.dto.DelinquentResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceAccountResponse;
import net.datatecsolution.admintools.domain.dto.LedgerEntryResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptResponse;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaPorCobrarCRUD;
import net.datatecsolution.admintools.persistence.crud.ReciboPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrar;
import net.datatecsolution.admintools.persistence.entity.ReciboPago;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * US-033 — Modulo de cuentas por cobrar. Expone por REST la misma logica que
 * el Swing legacy, sobre las mismas tablas de la BD comun. El saldo es el del
 * ultimo movimiento de {@code cuentas_por_cobrar} (== funcion
 * {@code f_saldo_cliente}); la morosidad y el estado por factura reusan las
 * funciones MySQL existentes.
 *
 * Alcance US-033: lectura completa + abono a NIVEL CLIENTE (mirror de
 * {@code ReciboPagoDao.registrar(recibo)} + {@code CuentaPorCobrarDao.reguistrarDebito}).
 * La aplicacion del abono a una factura especifica y la generacion automatica
 * del saldo en la venta a credito son US-034.
 */
@Service
public class AccountsReceivableService {

    private final ClienteCRUD clienteCRUD;
    private final CuentaPorCobrarCRUD cuentaPorCobrarCRUD;
    private final ReciboPagoCRUD reciboPagoCRUD;
    private final CuentaFacturaCRUD cuentaFacturaCRUD;
    private final TransactionTemplate commonTx;

    public AccountsReceivableService(ClienteCRUD clienteCRUD,
                                     CuentaPorCobrarCRUD cuentaPorCobrarCRUD,
                                     ReciboPagoCRUD reciboPagoCRUD,
                                     CuentaFacturaCRUD cuentaFacturaCRUD,
                                     @Qualifier("transactionManager") PlatformTransactionManager commonTm) {
        this.clienteCRUD = clienteCRUD;
        this.cuentaPorCobrarCRUD = cuentaPorCobrarCRUD;
        this.reciboPagoCRUD = reciboPagoCRUD;
        this.cuentaFacturaCRUD = cuentaFacturaCRUD;
        this.commonTx = new TransactionTemplate(commonTm);
    }

    // ============================================================
    //                          lectura
    // ============================================================

    public BalanceResponse getBalance(int customerId) {
        Cliente cliente = requireCliente(customerId);
        BigDecimal saldo = currentSaldo(customerId);
        BigDecimal limite = nz(cliente.getLimiteCredito());
        return new BalanceResponse(
                cliente.getId(),
                cliente.getNombre(),
                limite,
                saldo,
                limite.subtract(saldo));
    }

    public Page<LedgerEntryResponse> getStatement(int customerId, Pageable pageable) {
        requireCliente(customerId);
        return cuentaPorCobrarCRUD.findByCodigoClienteOrderByIdDesc(customerId, pageable)
                .map(m -> new LedgerEntryResponse(
                        m.getId(), m.getFecha(), m.getDescripcion(),
                        m.getCredito(), m.getDebito(), m.getSaldo()));
    }

    public java.util.List<InvoiceAccountResponse> getInvoiceAccounts(int customerId) {
        requireCliente(customerId);
        return cuentaFacturaCRUD.findInvoiceAccountsByCustomer(customerId).stream()
                .map(v -> new InvoiceAccountResponse(
                        v.getCodigoCuenta(), v.getNoFactura(), v.getCodigoCaja(),
                        v.getFecha(), v.getFechaVencimiento(), v.getSaldo(), v.getNoDias()))
                .toList();
    }

    public Page<DelinquentResponse> listDelinquent(int minDays, Pageable pageable) {
        return cuentaFacturaCRUD.findDelinquent(minDays, pageable)
                .map(v -> new DelinquentResponse(
                        v.getCodigoCuenta(), v.getNoFactura(), v.getCodigoCaja(),
                        v.getCodigoCliente(), v.getNombreCliente(), v.getTelefono(),
                        v.getFecha(), v.getFechaVencimiento(), v.getSaldo(),
                        v.getNoDias(), v.getUltimoPago(), v.getNombreVendedor()));
    }

    public Page<ReceiptResponse> getReceipts(int customerId, Pageable pageable) {
        requireCliente(customerId);
        return reciboPagoCRUD.findByCodigoClienteOrderByNoReciboDesc(customerId, pageable)
                .map(AccountsReceivableService::toReceiptResponse);
    }

    // ============================================================
    //                    abono (nivel cliente)
    // ============================================================

    /**
     * Aplica un abono a nivel cliente. Replica el flujo del Swing dentro de
     * una transaccion sobre el datasource comun:
     *   1) lee el saldo actual (ultimo movimiento, 0 si no hay),
     *   2) inserta el recibo (saldo_anterio, saldo = anterior - monto),
     *   3) inserta el movimiento debito en cuentas_por_cobrar con descripcion
     *      "concepto con recibo no. N".
     */
    public ReceiptResponse applyPayment(int customerId, AbonoRequest request, String user) {
        Cliente cliente = requireCliente(customerId);
        BigDecimal monto = scale(request.monto());

        return commonTx.execute(status -> {
            BigDecimal saldoAnterior = currentSaldo(customerId);
            BigDecimal nuevoSaldo = scale(saldoAnterior.subtract(monto));
            String concepto = (request.concepto() == null || request.concepto().isBlank())
                    ? "Abono" : request.concepto().trim();
            String ref = (request.ref() == null || request.ref().isBlank())
                    ? "NA" : request.ref().trim();

            ReciboPago recibo = new ReciboPago();
            recibo.setFecha(LocalDateTime.now());
            recibo.setCodigoCliente(cliente.getId());
            recibo.setTotal(monto);
            recibo.setConcepto(concepto);
            recibo.setUsuario(user == null ? "SYSTEM" : user);
            recibo.setSaldoAnterior(saldoAnterior);
            recibo.setSaldo(nuevoSaldo);
            recibo.setRef(ref);
            recibo.setTotalLetras("NA");
            ReciboPago saved = reciboPagoCRUD.save(recibo);

            // movimiento debito en el libro del cliente (mirror reguistrarDebito)
            CuentaPorCobrar movimiento = new CuentaPorCobrar();
            movimiento.setFecha(LocalDate.now());
            movimiento.setCodigoCliente(cliente.getId());
            movimiento.setDescripcion(concepto + " con recibo no. " + saved.getNoRecibo());
            movimiento.setCredito(BigDecimal.ZERO);
            movimiento.setDebito(monto);
            movimiento.setSaldo(nuevoSaldo);
            cuentaPorCobrarCRUD.save(movimiento);

            return toReceiptResponse(saved);
        });
    }

    // ============================================================
    //                          helpers
    // ============================================================

    private Cliente requireCliente(int customerId) {
        return clienteCRUD.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + customerId + " no encontrado"));
    }

    /** Saldo actual = saldo del ultimo movimiento; 0 si el cliente no tiene libro. */
    private BigDecimal currentSaldo(int customerId) {
        return cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(customerId)
                .map(CuentaPorCobrar::getSaldo)
                .map(AccountsReceivableService::nz)
                .orElse(BigDecimal.ZERO);
    }

    private static ReceiptResponse toReceiptResponse(ReciboPago r) {
        return new ReceiptResponse(
                r.getNoRecibo(), r.getFecha(), r.getCodigoCliente(), r.getTotal(),
                r.getConcepto(), r.getRef(), r.getUsuario(),
                r.getSaldoAnterior(), r.getSaldo());
    }

    private static BigDecimal scale(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

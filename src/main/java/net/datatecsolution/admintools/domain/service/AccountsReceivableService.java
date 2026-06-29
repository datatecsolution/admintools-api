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
import net.datatecsolution.admintools.persistence.crud.CuentaPorCobrarFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.InvoiceAccountView;
import net.datatecsolution.admintools.persistence.crud.ReciboPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.CuentaFactura;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrar;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrarFactura;
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
    private final CuentaPorCobrarFacturaCRUD cuentaPorCobrarFacturaCRUD;
    private final TransactionTemplate commonTx;

    public AccountsReceivableService(ClienteCRUD clienteCRUD,
                                     CuentaPorCobrarCRUD cuentaPorCobrarCRUD,
                                     ReciboPagoCRUD reciboPagoCRUD,
                                     CuentaFacturaCRUD cuentaFacturaCRUD,
                                     CuentaPorCobrarFacturaCRUD cuentaPorCobrarFacturaCRUD,
                                     @Qualifier("transactionManager") PlatformTransactionManager commonTm) {
        this.clienteCRUD = clienteCRUD;
        this.cuentaPorCobrarCRUD = cuentaPorCobrarCRUD;
        this.reciboPagoCRUD = reciboPagoCRUD;
        this.cuentaFacturaCRUD = cuentaFacturaCRUD;
        this.cuentaPorCobrarFacturaCRUD = cuentaPorCobrarFacturaCRUD;
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

            // movimiento debito en el libro mayor del cliente (mirror reguistrarDebito)
            CuentaPorCobrar movimiento = new CuentaPorCobrar();
            movimiento.setFecha(LocalDate.now());
            movimiento.setCodigoCliente(cliente.getId());
            movimiento.setDescripcion(concepto + " con recibo no. " + saved.getNoRecibo());
            movimiento.setCredito(BigDecimal.ZERO);
            movimiento.setDebito(monto);
            movimiento.setSaldo(nuevoSaldo);
            cuentaPorCobrarCRUD.save(movimiento);

            // Distribucion por factura, antigua-primero (mirror de
            // CtlCobro.registraPagoAfactura del Swing): cada factura con saldo
            // absorbe hasta su saldo y el resto pasa a la siguiente. Por cada
            // una se escribe un movimiento en cuentas_por_cobrar_facturas
            // (tipo_movimiento=2 = pago). Sin esto el saldo por factura nunca
            // baja y la factura sigue apareciendo como pendiente.
            // Orden antigua-primero por codigo_cuenta (orden de creacion de la
            // factura, como el Swing). NO por dias del ultimo pago: ese valor
            // se resetea al pagar y desordenaria la distribucion.
            java.util.List<InvoiceAccountView> facturas = cuentaFacturaCRUD
                    .findInvoiceAccountsByCustomer(customerId).stream()
                    .filter(v -> v.getSaldo() != null && v.getSaldo().signum() > 0)
                    .sorted(java.util.Comparator.comparingInt(InvoiceAccountView::getCodigoCuenta))
                    .toList();
            BigDecimal resto = monto;
            for (InvoiceAccountView f : facturas) {
                if (resto.signum() <= 0) break;
                BigDecimal saldoFactura = scale(f.getSaldo());
                BigDecimal pago = scale(saldoFactura.min(resto));
                resto = scale(resto.subtract(pago));
                CuentaPorCobrarFactura movFactura = new CuentaPorCobrarFactura();
                movFactura.setCodigoCuenta(f.getCodigoCuenta());
                movFactura.setFecha(LocalDate.now());
                movFactura.setDescripcion("Pago con recibo # " + saved.getNoRecibo());
                movFactura.setDebito(pago);
                movFactura.setCredito(BigDecimal.ZERO);
                movFactura.setSaldo(scale(saldoFactura.subtract(pago)));
                movFactura.setUsuario(user == null ? "SYSTEM" : user);
                movFactura.setTipoMovimiento(2);
                cuentaPorCobrarFacturaCRUD.save(movFactura);
            }

            return toReceiptResponse(saved);
        });
    }

    /**
     * US-041 — reversa la CxC de una factura a crédito anulada (mirror de
     * {@code createCreditoToCliente} del Swing). Abona el saldo REAL de la
     * factura (no el total: si hubo pagos parciales, no sobre-abona):
     *   1) pone el saldo de la factura (cuentas_por_cobrar_facturas) en 0;
     *   2) abona ese mismo saldo al libro mayor del cliente (recibo + débito).
     * Devuelve el monto abonado (0 si la factura no tenía cuenta/saldo).
     * Corre en su propia transacción común.
     */
    public BigDecimal reverseCreditInvoice(int customerId, int codigoCaja, int noFactura, String user) {
        return commonTx.execute(status -> {
            var cuentaOpt = cuentaFacturaCRUD.findFirstByNoFacturaAndCodigoCaja(noFactura, codigoCaja);
            if (cuentaOpt.isEmpty()) return BigDecimal.ZERO;
            int codigoCuenta = cuentaOpt.get().getCodigoCuenta();
            BigDecimal saldoFactura = scale(cuentaFacturaCRUD.saldoFactura(codigoCuenta));
            if (saldoFactura.signum() <= 0) return BigDecimal.ZERO;

            String concepto = "Anulación de factura # " + noFactura;
            String usuario = user == null ? "SYSTEM" : user;

            // 1) saldo de la factura -> 0 (cuentas_por_cobrar_facturas)
            CuentaPorCobrarFactura movFactura = new CuentaPorCobrarFactura();
            movFactura.setCodigoCuenta(codigoCuenta);
            movFactura.setFecha(LocalDate.now());
            movFactura.setDescripcion(concepto);
            movFactura.setDebito(saldoFactura);
            movFactura.setCredito(BigDecimal.ZERO);
            movFactura.setSaldo(BigDecimal.ZERO);
            movFactura.setUsuario(usuario);
            movFactura.setTipoMovimiento(2);
            cuentaPorCobrarFacturaCRUD.save(movFactura);

            // 2) abono al libro mayor del cliente por el saldo real (recibo + débito)
            BigDecimal saldoAnterior = currentSaldo(customerId);
            BigDecimal nuevoSaldo = scale(saldoAnterior.subtract(saldoFactura));

            ReciboPago recibo = new ReciboPago();
            recibo.setFecha(LocalDateTime.now());
            recibo.setCodigoCliente(customerId);
            recibo.setTotal(saldoFactura);
            recibo.setConcepto(concepto);
            recibo.setUsuario(usuario);
            recibo.setSaldoAnterior(saldoAnterior);
            recibo.setSaldo(nuevoSaldo);
            recibo.setRef("ANULACION");
            recibo.setTotalLetras("NA");
            ReciboPago saved = reciboPagoCRUD.save(recibo);

            CuentaPorCobrar mov = new CuentaPorCobrar();
            mov.setFecha(LocalDate.now());
            mov.setCodigoCliente(customerId);
            mov.setDescripcion(concepto + " con recibo no. " + saved.getNoRecibo());
            mov.setCredito(BigDecimal.ZERO);
            mov.setDebito(saldoFactura);
            mov.setSaldo(nuevoSaldo);
            cuentaPorCobrarCRUD.save(mov);

            return saldoFactura;
        });
    }

    // ============================================================
    //              US-034: integracion venta a credito
    // ============================================================

    /**
     * Genera el saldo en CxC de una venta a credito recien facturada. Mirror
     * de {@code FacturaDao} (tipo_factura==2): credito a nivel cliente, cuenta
     * por factura y credito a nivel factura; aplica el saldo a favor previo del
     * cliente si lo hubiera. Corre en una transaccion sobre el datasource comun.
     *
     * Lo invoca {@code InvoiceService.createFromOrder} DESPUES de commitear la
     * factura en la BD per-caja (de ahi llegan codigoCaja y noFactura).
     */
    public void postCreditSale(int customerId, int codigoCaja, int noFactura,
                               BigDecimal total, java.time.LocalDate fechaVencimiento, String user) {
        BigDecimal totalScaled = scale(total);
        String usuario = user == null ? "SYSTEM" : user;

        commonTx.executeWithoutResult(status -> {
            BigDecimal saldoAnterior = currentSaldo(customerId);

            // 1) credito a nivel cliente (saldo = anterior + total)
            CuentaPorCobrar credito = new CuentaPorCobrar();
            credito.setFecha(LocalDate.now());
            credito.setCodigoCliente(customerId);
            credito.setDescripcion("Venta a credito fact # " + noFactura);
            credito.setCredito(totalScaled);
            credito.setDebito(BigDecimal.ZERO);
            credito.setSaldo(scale(saldoAnterior.add(totalScaled)));
            cuentaPorCobrarCRUD.save(credito);

            // 2) cabecera de la cuenta por factura
            CuentaFactura cuenta = new CuentaFactura();
            cuenta.setCodigoCliente(customerId);
            cuenta.setNoFactura(noFactura);
            cuenta.setCodigoCaja(codigoCaja);
            cuenta.setFecha(LocalDate.now());
            cuenta.setFechaVencimiento(fechaVencimiento);
            CuentaFactura savedCuenta = cuentaFacturaCRUD.save(cuenta);

            // 3) credito a nivel factura (tipo_movimiento=1 saldo inicial)
            CuentaPorCobrarFactura credFactura = new CuentaPorCobrarFactura();
            credFactura.setCodigoCuenta(savedCuenta.getCodigoCuenta());
            credFactura.setFecha(LocalDate.now());
            credFactura.setDescripcion("Saldo inicial fact # " + noFactura);
            credFactura.setCredito(totalScaled);
            credFactura.setDebito(BigDecimal.ZERO);
            credFactura.setSaldo(totalScaled);
            credFactura.setUsuario(usuario);
            credFactura.setTipoMovimiento(1);
            cuentaPorCobrarFacturaCRUD.save(credFactura);

            // 4) saldo a favor previo del cliente (saldo < 0) -> abonarlo a la factura
            if (saldoAnterior.signum() < 0) {
                BigDecimal aFavor = saldoAnterior.negate();
                BigDecimal aplicado = aFavor.min(totalScaled);
                CuentaPorCobrarFactura debFavor = new CuentaPorCobrarFactura();
                debFavor.setCodigoCuenta(savedCuenta.getCodigoCuenta());
                debFavor.setFecha(LocalDate.now());
                debFavor.setDescripcion("Pago por saldo a favor del cliente");
                debFavor.setCredito(BigDecimal.ZERO);
                debFavor.setDebito(scale(aplicado));
                debFavor.setSaldo(scale(totalScaled.subtract(aplicado)));
                debFavor.setUsuario(usuario);
                debFavor.setTipoMovimiento(2);
                cuentaPorCobrarFacturaCRUD.save(debFavor);
            }
        });
    }

    /**
     * Aplica un abono a una factura especifica (cobro). Mirror de
     * {@code ReciboPagoDao.registrar(recibo, cuenta, agregarCtaGeneral=true)}:
     * recibo + debito a nivel factura (tipo 2) + debito a nivel cliente. El
     * recibo registra el saldo de la FACTURA (no el del cliente), igual que el Swing.
     */
    public ReceiptResponse applyInvoicePayment(int codigoCuenta, AbonoRequest request, String user) {
        BigDecimal monto = scale(request.monto());
        CuentaFactura cuenta = cuentaFacturaCRUD.findById(codigoCuenta)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cuenta de factura " + codigoCuenta + " no encontrada"));
        int customerId = cuenta.getCodigoCliente();

        return commonTx.execute(status -> {
            String concepto = (request.concepto() == null || request.concepto().isBlank())
                    ? "Abono" : request.concepto().trim();
            String ref = (request.ref() == null || request.ref().isBlank())
                    ? "NA" : request.ref().trim();

            BigDecimal saldoFacturaAnterior = currentSaldoFactura(codigoCuenta);
            BigDecimal nuevoSaldoFactura = scale(saldoFacturaAnterior.subtract(monto));

            // recibo (saldo de la factura, como el Swing en el path por factura)
            ReciboPago recibo = new ReciboPago();
            recibo.setFecha(LocalDateTime.now());
            recibo.setCodigoCliente(customerId);
            recibo.setTotal(monto);
            recibo.setConcepto(concepto);
            recibo.setUsuario(user == null ? "SYSTEM" : user);
            recibo.setSaldoAnterior(saldoFacturaAnterior);
            recibo.setSaldo(nuevoSaldoFactura);
            recibo.setRef(ref);
            recibo.setTotalLetras("NA");
            ReciboPago saved = reciboPagoCRUD.save(recibo);

            // debito a nivel factura (tipo_movimiento=2)
            CuentaPorCobrarFactura debFactura = new CuentaPorCobrarFactura();
            debFactura.setCodigoCuenta(codigoCuenta);
            debFactura.setFecha(LocalDate.now());
            debFactura.setDescripcion("Pago con recibo # " + saved.getNoRecibo() + ", ref " + ref);
            debFactura.setCredito(BigDecimal.ZERO);
            debFactura.setDebito(monto);
            debFactura.setSaldo(nuevoSaldoFactura);
            debFactura.setUsuario(user == null ? "SYSTEM" : user);
            debFactura.setTipoMovimiento(2);
            cuentaPorCobrarFacturaCRUD.save(debFactura);

            // debito a nivel cliente (agregarCtaGeneral=true)
            BigDecimal saldoClienteAnterior = currentSaldo(customerId);
            CuentaPorCobrar movCliente = new CuentaPorCobrar();
            movCliente.setFecha(LocalDate.now());
            movCliente.setCodigoCliente(customerId);
            movCliente.setDescripcion(concepto + " con recibo no. " + saved.getNoRecibo() + ", ref " + ref);
            movCliente.setCredito(BigDecimal.ZERO);
            movCliente.setDebito(monto);
            movCliente.setSaldo(scale(saldoClienteAnterior.subtract(monto)));
            cuentaPorCobrarCRUD.save(movCliente);

            return toReceiptResponse(saved);
        });
    }

    /** Historial de movimientos de una factura (trazabilidad). */
    public Page<LedgerEntryResponse> getInvoiceStatement(int codigoCuenta, Pageable pageable) {
        cuentaFacturaCRUD.findById(codigoCuenta)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cuenta de factura " + codigoCuenta + " no encontrada"));
        return cuentaPorCobrarFacturaCRUD.findByCodigoCuentaOrderByIdDesc(codigoCuenta, pageable)
                .map(m -> new LedgerEntryResponse(
                        m.getId(), m.getFecha(), m.getDescripcion(),
                        m.getCredito(), m.getDebito(), m.getSaldo()));
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

    /** Saldo actual de una factura = saldo del ultimo movimiento de esa cuenta. */
    private BigDecimal currentSaldoFactura(int codigoCuenta) {
        return cuentaPorCobrarFacturaCRUD.findTopByCodigoCuentaOrderByIdDesc(codigoCuenta)
                .map(CuentaPorCobrarFactura::getSaldo)
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

    /** Saldo + abonado de una factura a crédito (US-040, ticket). */
    public record Balance(BigDecimal saldo, BigDecimal abono) {}

    /**
     * US-040 — saldo pendiente y total abonado de una factura a crédito, para
     * el ticket "FACTURA A CRÉDITO". Devuelve {@code null} si la factura no
     * tiene cuenta en CxC (el caller usa saldo=total, abono=0).
     */
    public Balance getInvoiceBalance(int codigoCaja, int noFactura) {
        return cuentaFacturaCRUD.findFirstByNoFacturaAndCodigoCaja(noFactura, codigoCaja)
                .map(cf -> {
                    int cuenta = cf.getCodigoCuenta();
                    return new Balance(
                            scale(cuentaFacturaCRUD.saldoFactura(cuenta)),
                            scale(cuentaPorCobrarFacturaCRUD.sumAbonos(cuenta)));
                })
                .orElse(null);
    }
}

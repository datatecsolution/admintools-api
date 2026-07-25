package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.AbonoRequest;
import net.datatecsolution.admintools.domain.dto.BalanceResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptDetailResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptResponse;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaPorCobrarCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaPorCobrarFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.ReciboPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.CuentaFactura;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrar;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrarFactura;
import net.datatecsolution.admintools.persistence.entity.ReciboPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-033 — Unit tests del modulo de cuentas por cobrar. Foco en la matematica
 * del abono (saldoAnterior - monto, redondeo) y el saldo a partir del ultimo
 * movimiento, replicando la logica del Swing.
 */
@ExtendWith(MockitoExtension.class)
class AccountsReceivableServiceTest {

    @Mock private ClienteCRUD clienteCRUD;
    @Mock private CuentaPorCobrarCRUD cuentaPorCobrarCRUD;
    @Mock private ReciboPagoCRUD reciboPagoCRUD;
    @Mock private CuentaFacturaCRUD cuentaFacturaCRUD;
    @Mock private CuentaPorCobrarFacturaCRUD cuentaPorCobrarFacturaCRUD;
    @Mock private PlatformTransactionManager txManager;

    private AccountsReceivableService service() {
        return new AccountsReceivableService(
                clienteCRUD, cuentaPorCobrarCRUD, reciboPagoCRUD, cuentaFacturaCRUD,
                cuentaPorCobrarFacturaCRUD, txManager);
    }

    private Cliente cliente(int id, BigDecimal limite) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre("Cliente " + id);
        c.setLimiteCredito(limite == null ? BigDecimal.ZERO : limite);
        return c;
    }

    private CuentaPorCobrar movimiento(BigDecimal saldo) {
        CuentaPorCobrar m = new CuentaPorCobrar();
        m.setSaldo(saldo);
        return m;
    }

    @Test
    void getBalance_conMovimientos_calculaDisponible() {
        // limite 0 (el setter del entity legacy acumula, asi que arrancamos en 0)
        when(clienteCRUD.findById(7)).thenReturn(Optional.of(cliente(7, new BigDecimal("500.00"))));
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7))
                .thenReturn(Optional.of(movimiento(new BigDecimal("120.00"))));

        BalanceResponse r = service().getBalance(7);

        assertThat(r.saldo()).isEqualByComparingTo("120.00");
        assertThat(r.limiteCredito()).isEqualByComparingTo("500.00");
        assertThat(r.disponible()).isEqualByComparingTo("380.00");
    }

    @Test
    void getBalance_sinMovimientos_saldoCero() {
        when(clienteCRUD.findById(7)).thenReturn(Optional.of(cliente(7, BigDecimal.ZERO)));
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7)).thenReturn(Optional.empty());

        BalanceResponse r = service().getBalance(7);

        assertThat(r.saldo()).isEqualByComparingTo("0");
        assertThat(r.disponible()).isEqualByComparingTo("0");
    }

    // ---------- US-108 p3: re-lectura de recibo para reimpresion ----------

    @Test
    void getReceipt_mapeaCamposYNombreCliente() {
        ReciboPago r = new ReciboPago();
        r.setNoRecibo(45);
        r.setFecha(java.time.LocalDateTime.of(2026, 7, 20, 9, 15));
        r.setCodigoCliente(7);
        r.setTotal(new BigDecimal("250.00"));
        r.setConcepto("Abono");
        r.setRef("NA");
        r.setUsuario("tecnico");
        r.setSaldoAnterior(new BigDecimal("1000.00"));
        r.setSaldo(new BigDecimal("750.00"));
        when(reciboPagoCRUD.findById(45)).thenReturn(Optional.of(r));
        when(clienteCRUD.findById(7)).thenReturn(Optional.of(cliente(7, BigDecimal.ZERO)));

        ReceiptDetailResponse d = service().getReceipt(45);

        assertThat(d.noRecibo()).isEqualTo(45);
        assertThat(d.customerId()).isEqualTo(7);
        assertThat(d.nombreCliente()).isEqualTo("Cliente 7");
        assertThat(d.total()).isEqualByComparingTo("250.00");
        assertThat(d.saldoAnterior()).isEqualByComparingTo("1000.00");
        assertThat(d.saldo()).isEqualByComparingTo("750.00");
        assertThat(d.usuario()).isEqualTo("tecnico");
    }

    @Test
    void getReceipt_clienteBorrado_nombreNA() {
        ReciboPago r = new ReciboPago();
        r.setNoRecibo(46);
        r.setCodigoCliente(99);
        when(reciboPagoCRUD.findById(46)).thenReturn(Optional.of(r));
        when(clienteCRUD.findById(99)).thenReturn(Optional.empty());

        assertThat(service().getReceipt(46).nombreCliente()).isEqualTo("NA");
    }

    @Test
    void getReceipt_noExiste_lanza404() {
        when(reciboPagoCRUD.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getReceipt(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getBalance_clienteInexistente_lanza404() {
        when(clienteCRUD.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getBalance(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void applyPayment_bajaSaldoYRegistraReciboYMovimiento() {
        when(clienteCRUD.findById(7)).thenReturn(Optional.of(cliente(7, BigDecimal.ZERO)));
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7))
                .thenReturn(Optional.of(movimiento(new BigDecimal("100.00"))));
        // la transaccion ejecuta el callback (txManager mockeado, commit no-op)
        when(reciboPagoCRUD.save(any(ReciboPago.class))).thenAnswer(inv -> {
            ReciboPago r = inv.getArgument(0);
            r.setNoRecibo(5);
            return r;
        });

        ReceiptResponse resp = service().applyPayment(
                7, new AbonoRequest(new BigDecimal("30"), "Pago parcial", "TR-1"), "ronal");

        // recibo: saldoAnterior 100, saldo 70, total 30, usuario ronal
        assertThat(resp.noRecibo()).isEqualTo(5);
        assertThat(resp.total()).isEqualByComparingTo("30.00");
        assertThat(resp.saldoAnterior()).isEqualByComparingTo("100.00");
        assertThat(resp.saldo()).isEqualByComparingTo("70.00");
        assertThat(resp.usuario()).isEqualTo("ronal");

        // movimiento debito en el libro del cliente, saldo 70 y descripcion con el no. recibo
        ArgumentCaptor<CuentaPorCobrar> cap = ArgumentCaptor.forClass(CuentaPorCobrar.class);
        verify(cuentaPorCobrarCRUD).save(cap.capture());
        CuentaPorCobrar mov = cap.getValue();
        assertThat(mov.getDebito()).isEqualByComparingTo("30.00");
        assertThat(mov.getCredito()).isEqualByComparingTo("0");
        assertThat(mov.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(mov.getCodigoCliente()).isEqualTo(7);
        assertThat(mov.getDescripcion()).isEqualTo("Pago parcial con recibo no. 5");
    }

    @Test
    void applyPayment_sinMovimientosPrevios_saldoQuedaNegativo() {
        when(clienteCRUD.findById(7)).thenReturn(Optional.of(cliente(7, BigDecimal.ZERO)));
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7)).thenReturn(Optional.empty());
        when(reciboPagoCRUD.save(any(ReciboPago.class))).thenAnswer(inv -> {
            ReciboPago r = inv.getArgument(0);
            r.setNoRecibo(1);
            return r;
        });
        lenient().when(cuentaPorCobrarCRUD.save(any(CuentaPorCobrar.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse resp = service().applyPayment(
                7, new AbonoRequest(new BigDecimal("50"), null, null), "ronal");

        assertThat(resp.saldoAnterior()).isEqualByComparingTo("0");
        assertThat(resp.saldo()).isEqualByComparingTo("-50.00");
        assertThat(resp.concepto()).isEqualTo("Abono");
        assertThat(resp.ref()).isEqualTo("NA");
    }

    @Test
    void applyPayment_clienteInexistente_lanza404() {
        when(clienteCRUD.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().applyPayment(
                99, new AbonoRequest(new BigDecimal("10"), null, null), "ronal"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ============================================================
    //                          US-034
    // ============================================================

    private CuentaFactura cuentaFactura(int codigoCuenta, int customerId) {
        CuentaFactura cf = new CuentaFactura();
        cf.setCodigoCuenta(codigoCuenta);
        cf.setCodigoCliente(customerId);
        return cf;
    }

    private CuentaPorCobrarFactura movFactura(BigDecimal saldo) {
        CuentaPorCobrarFactura m = new CuentaPorCobrarFactura();
        m.setSaldo(saldo);
        return m;
    }

    @Test
    void postCreditSale_generaCreditoClienteYFactura() {
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7)).thenReturn(Optional.empty());
        when(cuentaFacturaCRUD.save(any(CuentaFactura.class))).thenAnswer(inv -> {
            CuentaFactura cf = inv.getArgument(0);
            cf.setCodigoCuenta(9);
            return cf;
        });

        service().postCreditSale(7, 4, 77, new BigDecimal("100"), LocalDate.of(2026, 7, 1), "ronal");

        // credito a nivel cliente: credito 100, saldo 100
        ArgumentCaptor<CuentaPorCobrar> cli = ArgumentCaptor.forClass(CuentaPorCobrar.class);
        verify(cuentaPorCobrarCRUD).save(cli.capture());
        assertThat(cli.getValue().getCredito()).isEqualByComparingTo("100.00");
        assertThat(cli.getValue().getSaldo()).isEqualByComparingTo("100.00");
        assertThat(cli.getValue().getCodigoCliente()).isEqualTo(7);

        // cuenta por factura con vencimiento y caja
        ArgumentCaptor<CuentaFactura> cf = ArgumentCaptor.forClass(CuentaFactura.class);
        verify(cuentaFacturaCRUD).save(cf.capture());
        assertThat(cf.getValue().getNoFactura()).isEqualTo(77);
        assertThat(cf.getValue().getCodigoCaja()).isEqualTo(4);
        assertThat(cf.getValue().getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 1));

        // credito a nivel factura: saldo 100, tipo 1, una sola save (sin saldo a favor)
        ArgumentCaptor<CuentaPorCobrarFactura> pf = ArgumentCaptor.forClass(CuentaPorCobrarFactura.class);
        verify(cuentaPorCobrarFacturaCRUD).save(pf.capture());
        assertThat(pf.getValue().getCredito()).isEqualByComparingTo("100.00");
        assertThat(pf.getValue().getSaldo()).isEqualByComparingTo("100.00");
        assertThat(pf.getValue().getTipoMovimiento()).isEqualTo(1);
        assertThat(pf.getValue().getCodigoCuenta()).isEqualTo(9);
    }

    @Test
    void postCreditSale_conSaldoAFavor_aplicaDebitoEnFactura() {
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7))
                .thenReturn(Optional.of(movimiento(new BigDecimal("-30.00"))));
        when(cuentaFacturaCRUD.save(any(CuentaFactura.class))).thenAnswer(inv -> {
            CuentaFactura cf = inv.getArgument(0);
            cf.setCodigoCuenta(9);
            return cf;
        });

        service().postCreditSale(7, 4, 77, new BigDecimal("100"), LocalDate.of(2026, 7, 1), "ronal");

        // cliente: credito 100 sobre saldo -30 => saldo 70
        ArgumentCaptor<CuentaPorCobrar> cli = ArgumentCaptor.forClass(CuentaPorCobrar.class);
        verify(cuentaPorCobrarCRUD).save(cli.capture());
        assertThat(cli.getValue().getSaldo()).isEqualByComparingTo("70.00");

        // factura: 2 movimientos (credito inicial 100 + debito por saldo a favor 30 => saldo 70)
        ArgumentCaptor<CuentaPorCobrarFactura> pf = ArgumentCaptor.forClass(CuentaPorCobrarFactura.class);
        verify(cuentaPorCobrarFacturaCRUD, times(2)).save(pf.capture());
        CuentaPorCobrarFactura debFavor = pf.getAllValues().get(1);
        assertThat(pf.getAllValues().get(0).getCredito()).isEqualByComparingTo("100.00");
        assertThat(debFavor.getDebito()).isEqualByComparingTo("30.00");
        assertThat(debFavor.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(debFavor.getTipoMovimiento()).isEqualTo(2);
    }

    @Test
    void applyInvoicePayment_bajaSaldoFacturaYCliente() {
        when(cuentaFacturaCRUD.findById(9)).thenReturn(Optional.of(cuentaFactura(9, 7)));
        when(cuentaPorCobrarFacturaCRUD.findTopByCodigoCuentaOrderByIdDesc(9))
                .thenReturn(Optional.of(movFactura(new BigDecimal("100.00"))));
        when(cuentaPorCobrarCRUD.findTopByCodigoClienteOrderByIdDesc(7))
                .thenReturn(Optional.of(movimiento(new BigDecimal("100.00"))));
        when(reciboPagoCRUD.save(any(ReciboPago.class))).thenAnswer(inv -> {
            ReciboPago r = inv.getArgument(0);
            r.setNoRecibo(3);
            return r;
        });

        ReceiptResponse resp = service().applyInvoicePayment(
                9, new AbonoRequest(new BigDecimal("40"), "Cobro", "REF-9"), "ronal");

        // el recibo registra el saldo de la FACTURA (100 -> 60), como el Swing
        assertThat(resp.noRecibo()).isEqualTo(3);
        assertThat(resp.saldoAnterior()).isEqualByComparingTo("100.00");
        assertThat(resp.saldo()).isEqualByComparingTo("60.00");

        // debito a nivel factura: tipo 2, saldo 60
        ArgumentCaptor<CuentaPorCobrarFactura> pf = ArgumentCaptor.forClass(CuentaPorCobrarFactura.class);
        verify(cuentaPorCobrarFacturaCRUD).save(pf.capture());
        assertThat(pf.getValue().getDebito()).isEqualByComparingTo("40.00");
        assertThat(pf.getValue().getSaldo()).isEqualByComparingTo("60.00");
        assertThat(pf.getValue().getTipoMovimiento()).isEqualTo(2);

        // debito a nivel cliente: saldo 100 -> 60
        ArgumentCaptor<CuentaPorCobrar> cliCap = ArgumentCaptor.forClass(CuentaPorCobrar.class);
        verify(cuentaPorCobrarCRUD).save(cliCap.capture());
        assertThat(cliCap.getValue().getDebito()).isEqualByComparingTo("40.00");
        assertThat(cliCap.getValue().getSaldo()).isEqualByComparingTo("60.00");
    }

    @Test
    void applyInvoicePayment_cuentaInexistente_lanza404() {
        when(cuentaFacturaCRUD.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().applyInvoicePayment(
                99, new AbonoRequest(new BigDecimal("10"), null, null), "ronal"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

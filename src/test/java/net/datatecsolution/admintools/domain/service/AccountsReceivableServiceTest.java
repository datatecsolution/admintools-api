package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.AbonoRequest;
import net.datatecsolution.admintools.domain.dto.BalanceResponse;
import net.datatecsolution.admintools.domain.dto.ReceiptResponse;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.CuentaPorCobrarCRUD;
import net.datatecsolution.admintools.persistence.crud.ReciboPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrar;
import net.datatecsolution.admintools.persistence.entity.ReciboPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    @Mock private PlatformTransactionManager txManager;

    private AccountsReceivableService service() {
        return new AccountsReceivableService(
                clienteCRUD, cuentaPorCobrarCRUD, reciboPagoCRUD, cuentaFacturaCRUD, txManager);
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
}

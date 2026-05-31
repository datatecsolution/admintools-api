package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrarFactura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * US-034 — Acceso al libro mayor de cuentas por cobrar a nivel factura.
 */
public interface CuentaPorCobrarFacturaCRUD extends JpaRepository<CuentaPorCobrarFactura, Integer> {

    /**
     * Ultimo movimiento de la factura = saldo actual de esa cuenta. Equivale a
     * la funcion MySQL {@code f_saldo_factura_cliente(codigo_cuenta)}.
     */
    Optional<CuentaPorCobrarFactura> findTopByCodigoCuentaOrderByIdDesc(Integer codigoCuenta);

    /** Historial de movimientos de la factura, mas reciente primero. */
    Page<CuentaPorCobrarFactura> findByCodigoCuentaOrderByIdDesc(Integer codigoCuenta, Pageable pageable);
}

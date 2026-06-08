package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CuentaPorCobrar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * US-033 — Acceso al libro mayor de cuentas por cobrar a nivel cliente.
 */
public interface CuentaPorCobrarCRUD extends JpaRepository<CuentaPorCobrar, Integer> {

    /**
     * Ultimo movimiento del cliente = saldo actual. Equivale a la funcion
     * MySQL {@code f_saldo_cliente(codigo_cliente)} usada por el Swing.
     */
    Optional<CuentaPorCobrar> findTopByCodigoClienteOrderByIdDesc(Integer codigoCliente);

    /** Estado de cuenta (movimientos del cliente, mas reciente primero). */
    Page<CuentaPorCobrar> findByCodigoClienteOrderByIdDesc(Integer codigoCliente, Pageable pageable);
}

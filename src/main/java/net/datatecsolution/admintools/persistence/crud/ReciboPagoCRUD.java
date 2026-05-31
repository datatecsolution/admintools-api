package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ReciboPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * US-033 — Acceso a recibos de pago (cabecera de abonos) a nivel cliente.
 */
public interface ReciboPagoCRUD extends JpaRepository<ReciboPago, Integer> {

    /** Historial de recibos del cliente, mas reciente primero. */
    Page<ReciboPago> findByCodigoClienteOrderByNoReciboDesc(Integer codigoCliente, Pageable pageable);
}

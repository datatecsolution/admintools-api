package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * US-032 — Acceso al catalogo de metodos de pago.
 */
public interface TipoPagoCRUD extends JpaRepository<TipoPago, Integer> {
}

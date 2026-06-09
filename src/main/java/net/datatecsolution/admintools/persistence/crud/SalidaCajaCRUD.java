package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.SalidaCaja;
import org.springframework.data.jpa.repository.JpaRepository;

/** Salidas de efectivo (los totales del cierre van por JdbcTemplate). */
public interface SalidaCajaCRUD extends JpaRepository<SalidaCaja, Integer> {
}

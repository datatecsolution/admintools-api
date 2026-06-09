package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.EntradaCaja;
import org.springframework.data.jpa.repository.JpaRepository;

/** Entradas de efectivo (los totales del cierre van por JdbcTemplate). */
public interface EntradaCajaCRUD extends JpaRepository<EntradaCaja, Integer> {
}

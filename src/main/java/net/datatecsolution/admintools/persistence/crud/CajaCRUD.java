package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Sprint 4.5 fix — repo de cajas (catalogo, read-only desde el panel).
 */
public interface CajaCRUD extends JpaRepository<Caja, Integer> {
}

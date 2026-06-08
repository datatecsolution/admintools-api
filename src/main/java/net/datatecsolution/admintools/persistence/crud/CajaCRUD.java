package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Sprint 4.5 fix — repo de cajas (catalogo, read-only desde el panel).
 */
public interface CajaCRUD extends JpaRepository<Caja, Integer> {

    /**
     * US-034 — resuelve el codigo_caja numerico desde el nombre de BD del
     * tenant (TenantContext guarda nombre_db, no el codigo).
     */
    Optional<Caja> findByNombreDb(String nombreDb);
}

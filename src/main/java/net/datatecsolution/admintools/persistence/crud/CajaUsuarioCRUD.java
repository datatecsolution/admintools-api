package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import net.datatecsolution.admintools.persistence.entity.CajaUsuarioKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Sprint 4.5 fix — asignaciones caja ↔ usuario.
 */
public interface CajaUsuarioCRUD extends JpaRepository<CajaUsuario, CajaUsuarioKey> {

    /** Todas las cajas asignadas a un username. */
    List<CajaUsuario> findByIdUsuario(String usuario);

    /** Borra todas las asignaciones de un usuario (al hacer replace). */
    void deleteByIdUsuario(String usuario);
}

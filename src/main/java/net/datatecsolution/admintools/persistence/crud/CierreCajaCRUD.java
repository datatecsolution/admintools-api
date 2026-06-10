package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Cierre de caja — repo de turnos. Mirror de
 * {@code CierreCajaDao.getCierreUltimoUser()} del Swing.
 */
public interface CierreCajaCRUD extends JpaRepository<CierreCaja, Integer> {

    /** Ultimo cierre (turno) del usuario, abierto o cerrado. */
    Optional<CierreCaja> findFirstByUsuarioOrderByIdCierreDesc(String usuario);
}

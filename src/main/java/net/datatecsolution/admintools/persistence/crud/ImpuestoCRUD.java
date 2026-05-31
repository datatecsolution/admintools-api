package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Impuesto;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Sprint 4.5+ fix — repo del catalogo de impuestos.
 * Tabla pequenia (1-10 filas), read-only por ahora.
 */
public interface ImpuestoCRUD extends JpaRepository<Impuesto, Integer> {
}

package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DatosEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * US-031 — Acceso a los datos de la empresa (tabla single-row).
 */
public interface DatosEmpresaCRUD extends JpaRepository<DatosEmpresa, Integer> {
}

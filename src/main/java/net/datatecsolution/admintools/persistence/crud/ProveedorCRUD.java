package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorCRUD extends JpaRepository<Proveedor, Integer> {
}

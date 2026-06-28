package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.InventoryCountLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountLineCRUD extends JpaRepository<InventoryCountLine, Integer> {

    List<InventoryCountLine> findByCodigoInventarioCount(Integer codigoInventarioCount);
}

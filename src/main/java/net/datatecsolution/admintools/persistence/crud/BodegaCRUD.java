package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodegaCRUD extends JpaRepository<Bodega, Integer> {
}

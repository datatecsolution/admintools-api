package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloMasterCRUD extends JpaRepository<ArticuloMaster, Integer> {

    /** Busqueda paginada por nombre (LIKE %name%, case-insensitive por la collation por default). */
    Page<ArticuloMaster> findByArticuloContaining(String name, Pageable pageable);
}

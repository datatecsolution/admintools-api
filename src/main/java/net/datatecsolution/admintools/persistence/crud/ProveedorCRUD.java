package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorCRUD extends JpaRepository<Proveedor, Integer> {

    /**
     * Búsqueda paginada por nombre o dirección (réplica de
     * ProveedorDao.buscarPorNombre/buscarPorDireccion del Swing).
     */
    @Query("SELECT p FROM Proveedor p "
            + "WHERE LOWER(p.nombreProveedor) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "   OR LOWER(p.direccion)       LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Proveedor> search(@Param("q") String q, Pageable pageable);
}

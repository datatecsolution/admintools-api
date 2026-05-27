package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.EncabezadoFacturaCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface EncabezadoFacturaCompraCRUD
        extends JpaRepository<EncabezadoFacturaCompra, Integer> {

    /**
     * Busqueda paginada con filtros opcionales:
     *  - supplier=null acepta cualquier proveedor.
     *  - from=null no impone limite inferior; to=null no impone limite superior.
     */
    @Query("""
           SELECT e FROM EncabezadoFacturaCompra e
            WHERE (:supplier IS NULL OR e.codigoProveedor = :supplier)
              AND (:from     IS NULL OR e.fecha >= :from)
              AND (:to       IS NULL OR e.fecha <= :to)
            ORDER BY e.fecha DESC, e.numeroCompra DESC
           """)
    Page<EncabezadoFacturaCompra> search(
            @Param("supplier") Integer supplier,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}

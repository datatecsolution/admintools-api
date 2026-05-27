package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.EncabezadoRequisicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EncabezadoRequisicionCRUD
        extends JpaRepository<EncabezadoRequisicion, Integer> {

    @Query("""
           SELECT e FROM EncabezadoRequisicion e
            WHERE (:origin      IS NULL OR e.codigoDepartOrigen  = :origin)
              AND (:destination IS NULL OR e.codigoDepartDestino = :destination)
              AND (:from        IS NULL OR e.fecha >= :from)
              AND (:to          IS NULL OR e.fecha <= :to)
            ORDER BY e.fecha DESC, e.codigoRequisicion DESC
           """)
    Page<EncabezadoRequisicion> search(
            @Param("origin") Integer origin,
            @Param("destination") Integer destination,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}

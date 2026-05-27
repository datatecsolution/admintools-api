package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleDevolucionCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DetalleDevolucionCompraCRUD
        extends JpaRepository<DetalleDevolucionCompra, Integer> {

    /** Todas las devoluciones asociadas a una compra original. */
    List<DetalleDevolucionCompra> findByNumeroFactura(Integer numeroFactura);

    @Query("""
           SELECT d FROM DetalleDevolucionCompra d
            WHERE (:purchase IS NULL OR d.numeroFactura = :purchase)
              AND (:from     IS NULL OR d.fecha >= :from)
              AND (:to       IS NULL OR d.fecha <= :to)
            ORDER BY d.fecha DESC, d.codigoDevolucion DESC
           """)
    Page<DetalleDevolucionCompra> search(
            @Param("purchase") Integer purchase,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}

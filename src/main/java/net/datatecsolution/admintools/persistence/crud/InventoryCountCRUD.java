package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.InventoryCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InventoryCountCRUD extends JpaRepository<InventoryCount, Integer> {

    @Query("""
           SELECT c FROM InventoryCount c
            WHERE (:bodega IS NULL OR c.codigoBodega = :bodega)
              AND (:from   IS NULL OR c.fecha >= :from)
              AND (:to     IS NULL OR c.fecha <= :to)
            ORDER BY c.fecha DESC, c.codigoInventarioCount DESC
           """)
    Page<InventoryCount> search(
            @Param("bodega") Integer bodega,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}

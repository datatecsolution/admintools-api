package net.datatecsolution.admintools.persistence.tenant.crud;

import net.datatecsolution.admintools.persistence.tenant.entity.EncabezadoFactura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncabezadoFacturaCRUD extends JpaRepository<EncabezadoFactura, Integer> {

    Page<EncabezadoFactura> findAllByOrderByFechaDesc(Pageable pageable);
}

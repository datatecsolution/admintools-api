package net.datatecsolution.admintools.persistence.tenant.crud;

import net.datatecsolution.admintools.persistence.tenant.entity.DetalleFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleFacturaCRUD extends JpaRepository<DetalleFactura, Integer> {

    List<DetalleFactura> findByNumeroFactura(Integer numeroFactura);
}

package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleFacturaCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleFacturaCompraCRUD extends JpaRepository<DetalleFacturaCompra, Integer> {

    List<DetalleFacturaCompra> findByNumeroCompra(Integer numeroCompra);
}

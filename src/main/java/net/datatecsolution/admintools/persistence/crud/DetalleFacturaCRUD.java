package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleFactura;
import org.springframework.data.repository.CrudRepository;

public interface DetalleFacturaCRUD extends CrudRepository<DetalleFactura,Integer> {

//    void deleteDetalleFacturaByIdFactura(Integer idFactura);
}

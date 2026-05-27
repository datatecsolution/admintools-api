package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleOrden;
import org.springframework.data.repository.CrudRepository;

public interface DetalleOrdenCRUD extends CrudRepository<DetalleOrden,Integer> {
}

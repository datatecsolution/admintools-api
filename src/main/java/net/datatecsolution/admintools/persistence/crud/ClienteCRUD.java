package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ClienteCRUD extends CrudRepository<Cliente, Integer> {
    List<Cliente> findByNombre(String nombre);
    List<Cliente> findByNombreContainingOrderByNombreAsc(String nombre);
}

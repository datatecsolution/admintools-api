package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Empleado;
import org.springframework.data.repository.CrudRepository;

public interface EmpleadoCRUD extends CrudRepository<Empleado, Integer> {

    Empleado findById(int id);
}

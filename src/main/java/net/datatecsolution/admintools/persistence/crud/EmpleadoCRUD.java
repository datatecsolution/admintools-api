package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface EmpleadoCRUD extends JpaRepository<Empleado, Integer> {

    Empleado findById(int id);

    @Query(value = "SELECT empleados.codigo_empleado, " +
                " empleados.nombre, " +
                " empleados.apellido, " +
                " empleados.telefono, " +
                " empleados.correo, " +
                " empleados.direccion, " +
                " empleados.sueldo_base, " +
                " empleados.codigo_tipo_empleado " +
            " FROM empleados " +
                "INNER JOIN usuario ON empleados.codigo_empleado=usuario.codigo_empleado " +
            " WHERE usuario.usuario=?",nativeQuery = true)
    Optional<Empleado> findByUsuario(String user);
}

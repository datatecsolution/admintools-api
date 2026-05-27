package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Orden;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdenCRUD extends CrudRepository<Orden,Integer> {

    List<Orden> findByClienteIdOrderByFechaAsc(int idCliente);

    List<Orden> getAllByOrderByFechaDesc();
    List<Orden> findByFechaOrderByFechaDesc(LocalDate fecha);

    Orden findByIdFacturaAndUsuario(int idA, String user);


   List<Orden> findByFechaIsBetweenAndUsuarioOrderByFechaDesc(LocalDateTime fecha, LocalDateTime fecha2,String usuario);
}

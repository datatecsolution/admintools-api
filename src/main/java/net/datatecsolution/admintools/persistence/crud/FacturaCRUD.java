package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Articulo;
import net.datatecsolution.admintools.persistence.entity.Factura;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaCRUD extends CrudRepository<Factura,Integer> {

    List<Factura> findByClienteIdOrderByFechaAsc(int idCliente);

    List<Factura> getAllByOrderByFechaDesc();
    List<Factura> findByFechaOrderByFechaDesc(LocalDate fecha);

    Factura findByIdFacturaAndUsuario(int idA, String user);


   List<Factura> findByFechaIsBetweenAndUsuarioOrderByFechaDesc(LocalDateTime fecha, LocalDateTime fecha2,String usuario);
}

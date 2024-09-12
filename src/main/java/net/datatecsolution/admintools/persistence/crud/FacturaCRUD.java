package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Articulo;
import net.datatecsolution.admintools.persistence.entity.Factura;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface FacturaCRUD extends CrudRepository<Factura,Integer> {

    List<Factura> findByClienteIdOrderByFechaAsc(int idCliente);

    List<Factura> getAllByOrderByFechaDesc();
    List<Factura> findByFechaOrderByFechaDesc(LocalDate fecha);


   // List<Factura> findByFechaBetweenAAndEstado(LocalDate fecha,LocalDate fecha2,String estado);
}

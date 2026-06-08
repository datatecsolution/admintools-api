package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Orden;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdenCRUD extends CrudRepository<Orden,Integer> {

    List<Orden> findByClienteIdOrderByFechaAsc(int idCliente);

    List<Orden> getAllByOrderByFechaDesc();
    List<Orden> findByFechaOrderByFechaDesc(LocalDate fecha);

    Orden findByIdFacturaAndUsuario(int idA, String user);


   /**
    * Órdenes pendientes del día por usuario. Filtra {@code estado < estadoMax}
    * (1=guardada, 2=actualizada) para excluir las anuladas (estado 5, borrado
    * lógico) y cualquier otro estado >= 3, fiel al Swing.
    */
   List<Orden> findByFechaIsBetweenAndUsuarioAndEstadoLessThanOrderByFechaDesc(
           LocalDateTime fecha, LocalDateTime fecha2, String usuario, Integer estadoMax);

   /**
    * Borrado lógico: marca la orden con el estado dado (5=anulada) en vez de
    * eliminar la fila. Conserva la traza y mantiene íntegros los detalles.
    */
   @Modifying
   @Transactional
   @Query("UPDATE Orden o SET o.estado = :estado WHERE o.idFactura = :id")
   int updateEstado(@Param("id") int id, @Param("estado") int estado);
}

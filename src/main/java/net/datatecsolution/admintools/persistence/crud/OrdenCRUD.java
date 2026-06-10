package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

   /**
    * Una orden por id, visible para el usuario según la regla del Swing: el
    * vendedor (empleado) tiene {@code empleados.usuario = :usuario}, O la orden
    * la creó ese usuario ({@code encabezado_factura_temp.usuario = :usuario}).
    * Devuelve null si no existe o no es visible.
    */
   @Query(value =
           "SELECT e.* FROM encabezado_factura_temp e " +
           "LEFT JOIN empleados emp ON e.codigo_vendedor = emp.codigo_empleado " +
           "WHERE e.numero_factura = :id " +
           "AND (emp.usuario = :usuario OR e.usuario = :usuario) LIMIT 1",
           nativeQuery = true)
   Orden findByIdFacturaVisible(@Param("id") int id, @Param("usuario") String usuario);

   /**
    * Órdenes pendientes del día VISIBLES para el usuario, fiel al filtro del
    * Swing ({@code FacturaOrdenVentaDao.ordenesPorEmpleadosUsuarios}): el
    * vendedor (empleado) tiene {@code empleados.usuario = :usuario}, O la orden
    * la creó ese usuario. {@code estado < :estadoMax} excluye anuladas (5) y
    * cualquier estado >= 3; acotado al rango del día.
    */
   @Query(value =
           "SELECT e.* FROM encabezado_factura_temp e " +
           "LEFT JOIN empleados emp ON e.codigo_vendedor = emp.codigo_empleado " +
           "WHERE e.estado < :estadoMax AND e.fecha BETWEEN :inicio AND :fin " +
           "AND (emp.usuario = :usuario OR e.usuario = :usuario) " +
           "ORDER BY e.fecha DESC",
           nativeQuery = true)
   List<Orden> findPendientesDelDiaVisibles(
           @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin,
           @Param("usuario") String usuario, @Param("estadoMax") Integer estadoMax);

   /**
    * Pendientes visibles SIN filtro de fecha — semántica exacta del Swing
    * ({@code FacturaOrdenVentaDao}): estado &lt; 3, visibilidad por usuario
    * (creador o vendedor-empleado del usuario) y orden por número de factura
    * descendente. El {@code LIMIT 0,20} del Swing era solo su paginación:
    * aquí va por {@link Pageable}. El POS usa esto; /orders/today (con
    * fecha) queda para la app de pedidos.
    */
   @Query(value =
           "SELECT e.* FROM encabezado_factura_temp e " +
           "LEFT JOIN empleados emp ON e.codigo_vendedor = emp.codigo_empleado " +
           "WHERE e.estado < :estadoMax " +
           "AND (emp.usuario = :usuario OR e.usuario = :usuario) " +
           "ORDER BY e.numero_factura DESC",
           countQuery =
           "SELECT count(*) FROM encabezado_factura_temp e " +
           "LEFT JOIN empleados emp ON e.codigo_vendedor = emp.codigo_empleado " +
           "WHERE e.estado < :estadoMax " +
           "AND (emp.usuario = :usuario OR e.usuario = :usuario)",
           nativeQuery = true)
   Page<Orden> findPendientesVisibles(
           @Param("usuario") String usuario, @Param("estadoMax") Integer estadoMax,
           Pageable pageable);

   /**
    * Borrado lógico: marca la orden con el estado dado (5=anulada) en vez de
    * eliminar la fila. Conserva la traza y mantiene íntegros los detalles.
    */
   @Modifying
   @Transactional
   @Query("UPDATE Orden o SET o.estado = :estado WHERE o.idFactura = :id")
   int updateEstado(@Param("id") int id, @Param("estado") int estado);
}

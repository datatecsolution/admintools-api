package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> getAll();

    Order save(Order order,String user);

    List<Order> getByToday(String user);

    /** Pendientes del usuario SIN filtro de fecha (semántica Swing), paginado. */
    Page<Order> getPendientes(String user, Pageable pageable);

    Optional<Order> getOrderUser(int orderId,String user);

    /** Borrado lógico: marca la orden como anulada (estado 5), conserva la fila. */
    void delete(int orderId);

    /** Borrado físico: elimina la fila (y sus detalles por cascade). Solo la app de órdenes. */
    void deletePhysical(int orderId);
}

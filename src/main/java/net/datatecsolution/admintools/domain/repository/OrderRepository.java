package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> getAll();

    Order save(Order order,String user);

    List<Order> getByToday(String user);

    Optional<Order> getOrderUser(int orderId,String user);

    void delete(int orderId);
}

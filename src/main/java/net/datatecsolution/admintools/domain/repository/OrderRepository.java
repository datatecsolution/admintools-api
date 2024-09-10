package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Order;

import java.util.List;

public interface OrderRepository {
    List<Order> getAll();

    Order save(Order order);
}

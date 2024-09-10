package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getAll() {
        return orderRepository.getAll();
    }
    public Order save(Order order) {
        return orderRepository.save(order);
    }

}

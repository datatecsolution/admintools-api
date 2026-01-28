package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerService sellerService;

    public List<Order> getAll() {
        return orderRepository.getAll();
    }
    public Order save(Order order,String user) {
        //crear validacion de las ordenes

        Optional<Seller> seller= sellerService.findByUser(user);

        System.out.println("El id de la factura es ===============>"+order.getOrderId());

        //se verifica que la orden es nueva sino significa que es modificada
        if (order.getOrderId() == null) {
            order.setActive(1);
        }else{
            order.setActive(2);
        }



        if(seller.isPresent()) {
            order.setSellerId(seller.get().getId());
            //order.setActive(1);
            return orderRepository.save(order,user);
        }else{
            throw new RuntimeException("Something went wrong");
        }


    }
    public List<Order> findByToday(String user) {
        return orderRepository.getByToday(user);
    }

    public boolean delete(int orderId, String user) {

        return getOrderUser(orderId,user).map(order -> {
            orderRepository.delete(orderId);
            return true;
        }).orElse(false);
    }
    public Optional<Order> getOrderUser(int orderId, String user) {
        return orderRepository.getOrderUser(orderId,user);

    }
}

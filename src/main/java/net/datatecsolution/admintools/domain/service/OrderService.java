package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerService sellerService;

    public List<Order> getAll() {
        return orderRepository.getAll();
    }

    public Order save(Order order, String user) {
        Optional<Seller> seller = sellerService.findByUser(user);
        if (seller.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Vendedor no encontrado para el usuario autenticado");
        }

        log.debug("Guardando orden con id={} user={}", order.getOrderId(), user);

        if (order.getOrderId() == null) {
            // INSERT
            order.setActive(1);
        } else {
            // UPDATE: validar que la orden pertenezca al usuario autenticado.
            // Sin este check, un usuario con JWT valido puede sobrescribir
            // ordenes de otros vendedores enviando un orderId ajeno.
            Optional<Order> existing = orderRepository.getOrderUser(order.getOrderId(), user);
            if (existing.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Orden no encontrada o no pertenece al usuario");
            }
            order.setActive(2);
        }

        order.setSellerId(seller.get().getId());
        return orderRepository.save(order, user);
    }

    public List<Order> findByToday(String user) {
        return orderRepository.getByToday(user);
    }

    public boolean delete(int orderId, String user, boolean fisico) {
        return getOrderUser(orderId, user).map(order -> {
            // Físico solo lo pide la app de órdenes (?fisico=true); el POS no
            // manda el flag, así que cae en el lógico (estado 5 = anulada).
            if (fisico) {
                orderRepository.deletePhysical(orderId);
            } else {
                orderRepository.delete(orderId);
            }
            return true;
        }).orElse(false);
    }

    public Optional<Order> getOrderUser(int orderId, String user) {
        return orderRepository.getOrderUser(orderId, user);
    }
}

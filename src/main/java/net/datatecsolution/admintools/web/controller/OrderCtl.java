package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = { "http://201.190.38.238", "http://localhost:3000/" })
public class OrderCtl {
    @Autowired
    private OrderService orderService;

    // @GetMapping("/all")
    // public ResponseEntity<List<Order>> getAll() {
    // return new ResponseEntity<>(orderService.getAll(), HttpStatus.OK);
    // }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable("orderId") int orderId, @RequestParam String user) {
        return orderService.getOrderUser(orderId, user)
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('SELLER')")   // US-021: vendedor crea ordenes (no factura)
    public ResponseEntity<Object> save(@RequestBody Order order, java.security.Principal principal) {
        String user = principal.getName();

        try {
            Order savedOrder = orderService.save(order, user);
            return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            // Propagar el status real (401, 404, etc.) — sin esto el catch
            // generico de abajo lo convertiria en 500.
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar la orden: " + e.getMessage());
        }
    }

    /**
     * Órdenes pendientes del usuario SIN filtro de fecha — lo usa la Lista de
     * órdenes del POS (semántica del Swing: estado < 3 + visibilidad, por
     * número desc). /today (con fecha) queda para la app de pedidos.
     */
    @GetMapping("/pending")
    public ResponseEntity<org.springframework.data.domain.Page<Order>> getPending(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            java.security.Principal principal) {
        String user = principal.getName();
        return new ResponseEntity<>(
                orderService.findPendientes(user,
                        org.springframework.data.domain.PageRequest.of(page, Math.min(size, 200))),
                HttpStatus.OK);
    }

    @GetMapping("/today")
    public ResponseEntity<List<Order>> getByNow(java.security.Principal principal) {
        String user = principal.getName();
        return new ResponseEntity<>(orderService.findByToday(user), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('SELLER')")   // US-021: el dueno de la orden la borra (vendedor o superior)
    public ResponseEntity delete(@PathVariable("id") int orderId,
                                 @RequestParam(name = "fisico", defaultValue = "false") boolean fisico,
                                 java.security.Principal principal) {
        // se tiene que enviar el usuario para verificar que la orden le pertenece.
        // fisico=true (app de ordenes) elimina la fila; sin el flag (POS) anula
        // logicamente (estado 5).
        String user = principal.getName();

        if (orderService.delete(orderId, user, fisico)) {
            return new ResponseEntity(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

}

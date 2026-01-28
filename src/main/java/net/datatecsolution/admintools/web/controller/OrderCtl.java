package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.Product;
import net.datatecsolution.admintools.domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = {"http://201.190.38.238", "http://localhost:3000/"})
public class OrderCtl {
    @Autowired
    private OrderService orderService;

//    @GetMapping("/all")
//    public ResponseEntity<List<Order>> getAll() {
//        return new ResponseEntity<>(orderService.getAll(), HttpStatus.OK);
//    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order>  getProduct(@PathVariable("orderId") int orderId,@RequestParam String user) {
        return orderService.getOrderUser(orderId,user)
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping("/save")
    public ResponseEntity<Object>  save(@RequestBody Order order) {


        if (  order.getUser() == null||   order.getUser().isEmpty()) {
            String errorMessage = "Error:'usuario' es requerido.";
            new ResponseEntity<>(errorMessage,HttpStatus.BAD_REQUEST);
        }

        try {

            Order savedOrder = orderService.save(order, order.getUser());
            return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar la orden: " + e.getMessage());
        }


    }
    @GetMapping("/today")
    public ResponseEntity<List<Order>> getByNow(@RequestParam String user) {
        System.out.println("user: " + user);
        if (  user == null||  user.isEmpty()) {
            String errorMessage = "Error:'usuario' es requerido.";
            new ResponseEntity<>(errorMessage,HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(orderService.findByToday(user), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") int orderId,@RequestParam String user) {
        //se tiene que enviar el usuario para verificar que la orden le pertenece
        System.out.println("user: " + user);
        if (  user == null||  user.isEmpty()) {
            String errorMessage = "Error:'usuario' es requerido.";
            new ResponseEntity<>(errorMessage,HttpStatus.BAD_REQUEST);
        }

        if(orderService.delete(orderId,user)){
            return new ResponseEntity(HttpStatus.OK);
        }else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

}

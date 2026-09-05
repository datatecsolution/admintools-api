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
// US-049: CORS lo gobierna el bean global (env CORS_ALLOWED_ORIGINS); se quitó
// el @CrossOrigin hardcodeado.
public class OrderCtl {
    @Autowired
    private OrderService orderService;

    // @GetMapping("/all")
    // public ResponseEntity<List<Order>> getAll() {
    // return new ResponseEntity<>(orderService.getAll(), HttpStatus.OK);
    // }

    // US-049: IDOR cerrado — la pertenencia se resuelve con el usuario del JWT
    // (principal), NO con el query param `user` que controlaba el atacante
    // (antes cualquier autenticado leía órdenes ajenas con ?user=<víctima>).
    // El param `user` se conserva opcional e IGNORADO para no romper clientes
    // viejos que aún lo envíen.
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable("orderId") int orderId,
                                          @RequestParam(name = "user", required = false) String ignoredUser,
                                          java.security.Principal principal) {
        return orderService.getOrderUser(orderId, principal.getName())
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('SELLER')")   // US-021: vendedor crea ordenes (no factura)
    public ResponseEntity<Object> save(@RequestBody Order order, java.security.Principal principal) {
        String user = principal.getName();

        // US-049: sin catch-all propio — todo sube al GlobalExceptionHandler,
        // que ya mapea ResponseStatusException / InsufficientStockException
        // (409) a su status y oculta el detalle interno del resto.
        //
        // US-150 (única excepción puntual): si dos POST con el MISMO clientRef
        // corren en paralelo, el chequeo del service no ve al otro (aún sin
        // commit) y el perdedor revienta contra el UNIQUE de client_ref. Ese
        // caso ES un reintento: se relee la orden ganadora (transacción nueva,
        // la del save ya rodó atrás) y se devuelve como éxito.
        try {
            Order savedOrder = orderService.save(order, user);
            return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (order.getClientRef() != null && !order.getClientRef().isBlank()) {
                java.util.Optional<Order> existing = orderService.findByClientRef(order.getClientRef());
                if (existing.isPresent()) {
                    return new ResponseEntity<>(existing.get(), HttpStatus.OK);
                }
            }
            throw e;
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

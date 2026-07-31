package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.OrderDetails;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.dto.StockConflict;
import net.datatecsolution.admintools.domain.exception.InsufficientStockException;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import net.datatecsolution.admintools.persistence.crud.ArticuloCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private ArticuloCRUD articuloCRUD;

    @Autowired
    private ConfigUserFacturacionCRUD configUserFacturacionCRUD;

    @Autowired
    private CajaUsuarioCRUD cajaUsuarioCRUD;

    public List<Order> getAll() {
        return orderRepository.getAll();
    }

    /** US-109: caja efectiva del vendedor — (codigo, codigo_bodega). */
    record CajaVendedor(int codigo, int bodega) {}

    /**
     * US-109: misma resolución que el TenantInterceptor (cajas_usuarios
     * por_defecto → fallback legacy usuario.codigo_caja). Empty si el usuario
     * no tiene caja por ninguna vía — el caller decide el error (nunca caer
     * en silencio a la caja 1, que era el bug del DEFAULT).
     */
    private Optional<CajaVendedor> resolverCajaVendedor(String user) {
        List<Object[]> filas = cajaUsuarioCRUD.findCajaEfectiva(user);
        if (filas.isEmpty()) {
            filas = cajaUsuarioCRUD.findCajaLegacy(user);
        }
        return filas.stream().findFirst()
                .map(fila -> new CajaVendedor(
                        ((Number) fila[0]).intValue(),
                        fila[1] == null ? 1 : ((Number) fila[1]).intValue()));
    }

    // @Transactional: el lock pesimista de US-074 debe vivir en la MISMA
    // transaccion que el insert/update de la orden — se libera al commit.
    // READ_COMMITTED es OBLIGATORIO para que el guard funcione: con el
    // REPEATABLE READ default, el read-view se crea en la PRIMERA lectura de
    // la tx (el findByUser de abajo), y la consulta de disponible que corre
    // DESPUES de adquirir el lock seguiria viendo el snapshot viejo — dos
    // saves concurrentes pasarian ambos. Con READ_COMMITTED cada statement
    // toma snapshot fresco y el segundo ve la orden commiteada del primero
    // (verificado con el test concurrente del DoD).
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Order save(Order order, String user) {
        Optional<Seller> seller = sellerService.findByUser(user);
        if (seller.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Vendedor no encontrado para el usuario autenticado");
        }

        // US-109: el pedido hereda la caja REAL del vendedor — sin esto,
        // codigo_caja quedaba en su DEFAULT 1 y la reserva de
        // f_existencia_y_ordenes caía siempre en la bodega de la caja 1,
        // fuera cual fuera la bodega del vendedor. Sin caja resoluble se
        // rechaza con mensaje claro (nunca default silencioso).
        CajaVendedor caja = resolverCajaVendedor(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "El vendedor no tiene caja asignada; asignala en Usuarios para poder crear pedidos"));

        log.debug("Guardando orden con id={} user={} caja={} bodega={}",
                order.getOrderId(), user, caja.codigo(), caja.bodega());

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

        // US-074: lock pesimista anti-sobreventa ANTES de persistir. Dos saves
        // concurrentes del mismo articulo se serializan en el FOR UPDATE: el
        // segundo ve la orden del primero ya commiteada y recibe 409.
        // US-109: el disponible se valida en la BODEGA de la caja del vendedor.
        List<StockConflict> conflictos = findStockConflicts(order, user, caja.bodega());
        if (!conflictos.isEmpty()) {
            throw new InsufficientStockException(conflictos);
        }

        order.setSellerId(seller.get().getId());
        return orderRepository.save(order, user, caja.codigo());
    }

    /**
     * US-074: bloqueo OPT-IN por usuario, misma semántica que el SP
     * crear_venta_kardex_v2 (V33): facturar_sin_inventario=0 → necesita stock;
     * =1 o sin row → permite sobreventa (comportamiento histórico, no toca a
     * los usuarios existentes). Disponible = saldo kardex − órdenes pendientes
     * (f_existencia_y_ordenes, la misma cifra que el vendedor ve como
     * existencia), reponiendo las líneas de la propia orden cuando es update.
     */
    private List<StockConflict> findStockConflicts(Order order, String user, int bodega) {
        if (order.getDetails() == null || order.getDetails().isEmpty()) {
            return List.of();
        }
        Integer permite = configUserFacturacionCRUD.findFacturarSinInventario(user).orElse(1);
        if (permite != 0) {
            return List.of();
        }

        // TreeMap → ids ascendentes SIEMPRE (orden de lock estable, anti-deadlock)
        Map<Integer, BigDecimal> pedidaPorArticulo = new TreeMap<>();
        for (OrderDetails d : order.getDetails()) {
            if (d.getAmount() == null) {
                continue;
            }
            pedidaPorArticulo.merge(d.getProductId(), d.getAmount(), BigDecimal::add);
        }
        if (pedidaPorArticulo.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = List.copyOf(pedidaPorArticulo.keySet());
        articuloCRUD.lockByIds(ids);

        int ordenExcluida = order.getOrderId() == null ? -1 : order.getOrderId();
        List<StockConflict> conflictos = new ArrayList<>();
        for (Object[] fila : articuloCRUD.findDisponibleParaOrden(ids, ordenExcluida, bodega)) {
            int codigoArticulo = ((Number) fila[0]).intValue();
            String nombre = (String) fila[1];
            BigDecimal disponible = fila[2] == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(fila[2].toString());
            BigDecimal pedida = pedidaPorArticulo.get(codigoArticulo);
            if (pedida.compareTo(disponible) > 0) {
                conflictos.add(new StockConflict(codigoArticulo, nombre, pedida, disponible));
            }
        }
        return conflictos;
    }

    /** Pendientes del usuario sin filtro de fecha (POS), paginado. */
    public org.springframework.data.domain.Page<Order> findPendientes(String user,
            org.springframework.data.domain.Pageable pageable) {
        return orderRepository.getPendientes(user, pageable);
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

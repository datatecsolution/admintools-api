package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import net.datatecsolution.admintools.persistence.crud.*;
import net.datatecsolution.admintools.persistence.entity.*;
import net.datatecsolution.admintools.persistence.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion del {@link OrderRepository} del dominio. Antes se llamaba
 * {@code FacturaRepository} porque el entity subyacente se llamaba
 * {@code Factura}, pero esta tabla ({@code encabezado_factura_temp}) guarda
 * ordenes, no facturas — el rename a {@link Orden} + {@link DetalleOrden}
 * dejo libre el nombre {@code Factura} para las definitivas de INV-8.
 */
@Repository
public class OrdenRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(OrdenRepository.class);

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    @Autowired
    private OrdenCRUD ordenCRUD;
    @Autowired
    private EmpleadoCRUD empleadoCRUD;

    @Autowired
    private DetalleOrdenCRUD detalleOrdenCRUD;
    @Autowired
    private ArticuloCRUD articuloCRUD;

    @Autowired
    private PreciosArticuloCRUD preciosArticuloCRUD;

    @Autowired
    private OrderMapper mapper;
    @Override
    public List<Order> getAll() {
        List<Orden> ordenes = (List<Orden>) ordenCRUD.getAllByOrderByFechaDesc();
        return mapper.toOrders(ordenes);
    }

    @Override
    public Order save(Order order, String user) {
        Orden orden = mapper.toOrden(order);
        // SOLO si el payload no trae usuario (p.ej. POS). Si la app de pedidos lo
        // envia, se preserva — no se altera el comportamiento en produccion.
        if (orden.getUsuario() == null || orden.getUsuario().isBlank()) {
            orden.setUsuario(user);
        }
        log.debug("save() inicial idFactura={}", orden.getIdFactura());

        if (orden.getIdFactura() == null) {
            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            if (empleado.isPresent()) {
                orden.setVendedor(empleado.get());
                orden.setVendedorCod(empleado.get().getCodigo());
            }

            // se completa los detalles para poder guardarlos
            for (DetalleOrden detalleOrden : orden.getDetalles()) {
                Optional<Articulo> articulo = articuloCRUD.findById(detalleOrden.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleOrden.setArticulo(articulo.get());
                    detalleOrden.setCodigoArt(articulo.get().getArticuloId());
                    detalleOrden.setOrden(orden);
                }
            }

            orden.calcularTotales();
            Orden savedOrden = ordenCRUD.save(orden);
            return mapper.toOrder(savedOrden);
        } else {
            log.debug("save() actualizando idFactura={} con {} detalles",
                    orden.getIdFactura(), orden.getDetalles().size());

            // Preservar fecha original: la React no envia el campo `date` en
            // el payload de update, asi que `orden.fecha` viene null del
            // mapper. @PrePersist solo dispara en INSERT, NO en MERGE, asi
            // que sin esto JPA escribiria fecha=NULL → MySQL coerce a
            // '0000-00-00' → la orden desaparece del filtro getByToday.
            if (orden.getFecha() == null) {
                ordenCRUD.findById(orden.getIdFactura())
                        .ifPresent(existente -> orden.setFecha(existente.getFecha()));
            }

            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            empleado.ifPresent(orden::setVendedor);

            // se completa los detalles para poder guardarlos
            for (DetalleOrden detalleOrden : orden.getDetalles()) {
                log.debug("save() detalle codigoArt={}", detalleOrden.getCodigoArt());
                Optional<Articulo> articulo = articuloCRUD.findById(detalleOrden.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleOrden.setArticulo(articulo.get());
                    detalleOrden.setCodigoArt(articulo.get().getArticuloId());
                }
                detalleOrden.setOrden(orden);
            }

            orden.calcularTotales();
            return mapper.toOrder(ordenCRUD.save(orden));
        }
    }

    @Override
    public List<Order> getByToday(String user) {
        // Calcula "hoy" en la zona horaria del cliente (Honduras por default).
        // Convierte los limites al JVM-TZ antes de pasarlos al driver MySQL,
        // porque con serverTimezone=GMT-6 en la URL el driver aplica
        // conversion sobre LocalDateTime (a pesar de ser tipo naive). Si
        // pasamos "Honduras 00:00" como LocalDateTime sin TZ, el driver lo
        // interpreta como JVM-local (UTC) y lo convierte a GMT-6, terminando
        // en "May 16 18:00" en lugar de "May 17 00:00". Pre-convertir a
        // JVM-TZ neutraliza este efecto.
        ZoneId hondurasZone = ZoneId.of(timezoneId);
        ZoneId jvmZone = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(hondurasZone);
        LocalDateTime inicioDelDia = hoy.atStartOfDay(hondurasZone)
                .withZoneSameInstant(jvmZone)
                .toLocalDateTime();
        LocalDateTime finDelDia = hoy.atTime(23, 59, 59).atZone(hondurasZone)
                .withZoneSameInstant(jvmZone)
                .toLocalDateTime();
        // Visibilidad fiel al Swing: la orden la creó el usuario (encabezado.
        // usuario) O su vendedor (empleado) está marcado con ese usuario
        // (empleados.usuario). estado < 3 → solo pendientes (1=guardada,
        // 2=actualizada); excluye anuladas (5) y cualquier estado >= 3.
        List<Orden> ordenes = ordenCRUD.findPendientesDelDiaVisibles(
                inicioDelDia, finDelDia, user, 3);
        aplicarPreciosUsuario(ordenes, user);
        return mapper.toOrders(ordenes);
    }

    @Override
    public org.springframework.data.domain.Page<Order> getPendientes(String user,
            org.springframework.data.domain.Pageable pageable) {
        // Semántica Swing (FacturaOrdenVentaDao): estado < 3 + visibilidad por
        // usuario, SIN filtro de fecha, por número desc. La paginación
        // reemplaza el LIMIT 0,20 fijo del Swing.
        org.springframework.data.domain.Page<Orden> page =
                ordenCRUD.findPendientesVisibles(user, 3, pageable);
        aplicarPreciosUsuario(page.getContent(), user);
        return page.map(mapper::toOrder);
    }

    /** Reemplaza los precios de cada línea por los que puede aplicar el usuario. */
    private void aplicarPreciosUsuario(List<Orden> ordenes, String user) {
        for (Orden orden : ordenes) {
            for (DetalleOrden detalleOrden : orden.getDetalles()) {
                List<PrecioArticulo> precios =
                        preciosArticuloCRUD.findPrecioUser(detalleOrden.getCodigoArt(), user);
                if (precios != null) {
                    detalleOrden.getArticulo().setPrecioArticulos(precios);
                } else {
                    detalleOrden.getArticulo().getPrecioArticulos().clear();
                    detalleOrden.getArticulo().setPrecioArticulos(new ArrayList<>());
                }
            }
        }
    }
    @Override
    public Optional<Order> getOrderUser(int orderId, String user){
        // ofNullable + map: si la orden no existe o no es VISIBLE para el usuario
        // (regla Swing: empleados.usuario O encabezado.usuario) devuelve
        // Optional.empty() → el controller responde 404 limpio. Antes era
        // Optional.of(mapper.toOrder(null)) → NPE → 500.
        return Optional.ofNullable(ordenCRUD.findByIdFacturaVisible(orderId, user))
                .map(mapper::toOrder);
    }

    @Override
    public void delete(int orderId) {
        // Borrado LÓGICO (estado 5 = anulada): NO se elimina la fila — conserva
        // la traza, fiel al Swing. getByToday filtra estado<3, así que la orden
        // anulada deja de aparecer en el listado.
        ordenCRUD.updateEstado(orderId, 5);
    }

    @Override
    public void deletePhysical(int orderId) {
        // Borrado FÍSICO: elimina la fila (cascade borra los detalles). Lo usa
        // la app de órdenes; el POS solo hace el lógico de arriba.
        ordenCRUD.deleteById(orderId);
    }

}

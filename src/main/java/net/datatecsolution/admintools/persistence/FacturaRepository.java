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

@Repository
public class FacturaRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(FacturaRepository.class);

    @Value("${app.timezone:America/Tegucigalpa}")
    private String timezoneId;

    @Autowired
    private FacturaCRUD facturaCRUD;
    @Autowired
    private EmpleadoCRUD empleadoCRUD;

    @Autowired
    private DetalleFacturaCRUD detalleFacturaCRUD;
    @Autowired
    private ArticuloCRUD articuloCRUD;

    @Autowired
    private PreciosArticuloCRUD preciosArticuloCRUD;

    @Autowired
    private OrderMapper mapper;
    @Override
    public List<Order> getAll() {
        List<Factura> facturas = (List<Factura>) facturaCRUD.getAllByOrderByFechaDesc();
        return mapper.toOrders(facturas);
    }

    @Override
    public Order save(Order order, String user) {
        Factura factura = mapper.toFactura(order);
        log.debug("save() inicial idFactura={}", factura.getIdFactura());

        if (factura.getIdFactura() == null) {
            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            if (empleado.isPresent()) {
                factura.setVendedor(empleado.get());
                factura.setVendedorCod(empleado.get().getCodigo());
            }

            // se completa los detalles para poder guardarlos
            for (DetalleFactura detalleFactura : factura.getDetalles()) {
                Optional<Articulo> articulo = articuloCRUD.findById(detalleFactura.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleFactura.setArticulo(articulo.get());
                    detalleFactura.setCodigoArt(articulo.get().getArticuloId());
                    detalleFactura.setFactura(factura);
                }
            }

            factura.calcularTotales();
            Factura savedFactura = facturaCRUD.save(factura);
            return mapper.toOrder(savedFactura);
        } else {
            log.debug("save() actualizando idFactura={} con {} detalles",
                    factura.getIdFactura(), factura.getDetalles().size());

            // Preservar fecha original: la React no envia el campo `date` en
            // el payload de update, asi que `factura.fecha` viene null del
            // mapper. @PrePersist solo dispara en INSERT, NO en MERGE, asi
            // que sin esto JPA escribiria fecha=NULL → MySQL coerce a
            // '0000-00-00' → la orden desaparece del filtro getByToday.
            if (factura.getFecha() == null) {
                facturaCRUD.findById(factura.getIdFactura())
                        .ifPresent(existente -> factura.setFecha(existente.getFecha()));
            }

            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            empleado.ifPresent(factura::setVendedor);

            // se completa los detalles para poder guardarlos
            for (DetalleFactura detalleFactura : factura.getDetalles()) {
                log.debug("save() detalle codigoArt={}", detalleFactura.getCodigoArt());
                Optional<Articulo> articulo = articuloCRUD.findById(detalleFactura.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleFactura.setArticulo(articulo.get());
                    detalleFactura.setCodigoArt(articulo.get().getArticuloId());
                }
                detalleFactura.setFactura(factura);
            }

            factura.calcularTotales();
            return mapper.toOrder(facturaCRUD.save(factura));
        }
    }

    @Override
    public List<Order> getByToday(String user) {
        // Usar ZoneId explicito en lugar de LocalDate.now() (default JVM)
        // garantiza que "hoy" sea consistente independientemente del TZ del
        // contenedor Docker (frecuentemente UTC, desfasado 6h del horario
        // local de Honduras).
        ZoneId zone = ZoneId.of(timezoneId);
        LocalDate hoy = LocalDate.now(zone);
        LocalDateTime inicioDelDia = hoy.atStartOfDay();
        LocalDateTime finDelDia = hoy.atTime(23, 59, 59);
        List<Factura> facturas = (List<Factura>) facturaCRUD.findByFechaIsBetweenAndUsuarioOrderByFechaDesc(inicioDelDia, finDelDia,user);

        //se recorre las facturas para cambiar los precios que puede cambiar el usuario
        for (Factura factura : facturas) {
            for (DetalleFactura detalleFactura : factura.getDetalles()) {

                List<PrecioArticulo> precios = new ArrayList<>();

                //se buscan los precios que puede aplicar el usuario
                precios = preciosArticuloCRUD.findPrecioUser(detalleFactura.getCodigoArt(), user);

                //se establece los precios

                //si los precios existen se aplican al articulo
                if (precios != null) {
                    detalleFactura.getArticulo().setPrecioArticulos(precios);
                } else {
                    detalleFactura.getArticulo().getPrecioArticulos().clear();
                    detalleFactura.getArticulo().setPrecioArticulos(new ArrayList<>());
                }
            }
        }
        return mapper.toOrders(facturas);
    }
    @Override
    public Optional<Order> getOrderUser(int orderId, String user){
        return Optional.of(mapper.toOrder(facturaCRUD.findByIdFacturaAndUsuario(orderId,user)));
    }

    @Override
    public void delete(int orderId) {
        facturaCRUD.deleteById(orderId);
    }

}

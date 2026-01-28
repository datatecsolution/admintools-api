package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import net.datatecsolution.admintools.persistence.crud.*;
import net.datatecsolution.admintools.persistence.entity.*;
import net.datatecsolution.admintools.persistence.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FacturaRepository implements OrderRepository {
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
    public Order save(Order order,String user) {
        Factura factura= mapper.toFactura(order);
        System.out.println("El codigo de factura es inicial======>>>>>>>>"+factura.getIdFactura());
        if(factura.getIdFactura()==null) {
            //System.out.println("El codigo de factura es ======>>>>>>>> null");
            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            if (empleado.isPresent()) {
                factura.setVendedor(empleado.get());
                factura.setVendedorCod(empleado.get().getCodigo());
            }
            //System.out.println("Cant de detalles======>>>>>>>>"+factura.getDetalles().size());
            //se completa los detalles para poder guardarlos
            for (DetalleFactura detalleFactura : factura.getDetalles()) {
                //System.out.println("Item id articulo ======>>>>>>>>"+detalleFactura.getCodigoArt());
                Optional<Articulo> articulo = articuloCRUD.findById(detalleFactura.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleFactura.setArticulo(articulo.get());
                    detalleFactura.setCodigoArt(articulo.get().getArticuloId());
                    detalleFactura.setFactura(factura);
                }

            }

            factura.calcularTotales();
            //se guarda los el encabezado de la orden
            Factura savedFactura = facturaCRUD.save(factura);

//            detalleFacturaCRUD.deleteDetalleFacturaByIdFactura(savedFactura.getIdFactura());
//
//
//            //se recorre de nuevo los detalles para guardarlos uno a uno
//            for (DetalleFactura detalleFactura : factura.getDetalles()) {
//                detalleFactura.setIdFactura(savedFactura.getIdFactura());
//                detalleFactura.setFactura(savedFactura);
//                detalleFacturaCRUD.save(detalleFactura);
//
//            }
            return mapper.toOrder(savedFactura);
        }else{
            System.out.println("El codigo de factura es ======>>>>>>>>"+factura.getIdFactura());
            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            empleado.ifPresent(factura::setVendedor);



            System.out.println("Cant de detalles======>>>>>>>>"+factura.getDetalles().size());


            //se completa los detalles para poder guardarlos
            for (DetalleFactura detalleFactura : factura.getDetalles()) {
                System.out.println("Item id articulo ======>>>>>>>>"+detalleFactura.getCodigoArt());
                Optional<Articulo> articulo = articuloCRUD.findById(detalleFactura.getCodigoArt());
                if (articulo.isPresent()) {
                    detalleFactura.setArticulo(articulo.get());
                    detalleFactura.setCodigoArt(articulo.get().getArticuloId());
                }
                detalleFactura.setFactura(factura);
                //detalleFactura.setIdFactura(factura.getIdFactura());

            }


            factura.calcularTotales();
           // detalleFacturaCRUD.deleteDetalleFacturaByIdFactura(factura.getIdFactura());

//            //se recorre de nuevo los detalles para guardarlos uno a uno
//            for (DetalleFactura detalleFactura : factura.getDetalles()) {
//                System.out.print(",Item cod articulo ======>>>>>>>>"+detalleFactura.getCodigoArt());
//                System.out.print(",Item cantidad ======>>>>>>>>"+detalleFactura.getCantidad());
//                System.out.println(",Item total ======>>>>>>>>"+detalleFactura.getTotal());
//                detalleFacturaCRUD.save(detalleFactura);
//
//            }
            return mapper.toOrder(facturaCRUD.save(factura));




        }
        //facturaCRUD.save(savedFactura);



    }

    @Override
    public List<Order> getByToday(String user) {
        LocalDateTime inicioDelDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDelDia = LocalDate.now().atTime(23, 59, 59);
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

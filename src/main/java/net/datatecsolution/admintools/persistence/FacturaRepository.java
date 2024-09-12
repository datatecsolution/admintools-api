package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import net.datatecsolution.admintools.persistence.crud.ArticuloCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleFacturaCRUD;
import net.datatecsolution.admintools.persistence.crud.EmpleadoCRUD;
import net.datatecsolution.admintools.persistence.crud.FacturaCRUD;
import net.datatecsolution.admintools.persistence.entity.Articulo;
import net.datatecsolution.admintools.persistence.entity.DetalleFactura;
import net.datatecsolution.admintools.persistence.entity.Empleado;
import net.datatecsolution.admintools.persistence.entity.Factura;
import net.datatecsolution.admintools.persistence.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private OrderMapper mapper;
    @Override
    public List<Order> getAll() {
        List<Factura> facturas = (List<Factura>) facturaCRUD.getAllByOrderByFechaDesc();
        return mapper.toOrders(facturas);
    }

    @Override
    public Order save(Order order) {
        Factura factura= mapper.toFactura(order);
        System.out.println("El codigo de factura es inicial======>>>>>>>>"+factura.getIdFactura());
        if(factura.getIdFactura()==null) {
            System.out.println("El codigo de factura es ======>>>>>>>> null");
            Optional<Empleado> empleado = empleadoCRUD.findById(order.getSellerId());
            if (empleado.isPresent()) {
                factura.setVendedor(empleado.get());
            }
            System.out.println("Cant de detalles======>>>>>>>>"+factura.getDetalles().size());
            //se completa los detalles para poder guardarlos
            for (DetalleFactura detalleFactura : factura.getDetalles()) {
                System.out.println("Item id articulo ======>>>>>>>>"+detalleFactura.getCodigoArt());
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

}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.OrderRepository;
import net.datatecsolution.admintools.persistence.crud.ArticuloCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-150 — idempotencia del guardado de órdenes.
 *
 * El bug que motiva esto (Sharon, jun-sep 2026): la respuesta del POST se
 * pierde en la red móvil ("Load failed"), el vendedor re-guarda y nace una
 * orden duplicada que caja factura dos veces (6 dobles cobros activos
 * detectados el 2026-09-04). Con clientRef, el reintento devuelve la orden
 * ya creada en vez de insertar otra.
 */
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SellerService sellerService;
    @Mock
    private ArticuloCRUD articuloCRUD;
    @Mock
    private ConfigUserFacturacionCRUD configUserFacturacionCRUD;
    @Mock
    private CajaVendedorService cajaVendedorService;

    @InjectMocks
    private OrderService service;

    private static final String USER = "ANGELO";
    private static final String REF = "9b2f0c1e-5b34-4a8e-9a51-2f4f9a3f0001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Seller seller = new Seller();
        seller.setId(29);
        when(sellerService.findByUser(USER)).thenReturn(Optional.of(seller));
        when(cajaVendedorService.resolver(USER))
                .thenReturn(Optional.of(new CajaVendedorService.CajaVendedor(2, 1)));
        // Sin guard de stock (comportamiento histórico por default).
        when(configUserFacturacionCRUD.findFacturarSinInventario(anyString()))
                .thenReturn(Optional.of(1));
    }

    private static Order ordenNueva(String clientRef) {
        Order order = new Order();
        order.setClientRef(clientRef);
        return order;
    }

    @Test
    void reintentoConMismoClientRefDevuelveLaOrdenExistenteSinInsertar() {
        Order existente = new Order();
        existente.setOrderId(105168);
        when(orderRepository.findByClientRef(REF)).thenReturn(Optional.of(existente));

        Order resultado = service.save(ordenNueva(REF), USER);

        assertEquals(105168, resultado.getOrderId());
        verify(orderRepository, never()).save(any(), anyString(), anyInt());
    }

    @Test
    void clientRefNuevoInsertaNormalmente() {
        when(orderRepository.findByClientRef(REF)).thenReturn(Optional.empty());
        Order guardada = new Order();
        guardada.setOrderId(106220);
        when(orderRepository.save(any(), eq(USER), eq(2))).thenReturn(guardada);

        Order resultado = service.save(ordenNueva(REF), USER);

        assertEquals(106220, resultado.getOrderId());
        verify(orderRepository).save(any(), eq(USER), eq(2));
    }

    @Test
    void sinClientRefConservaElComportamientoHistorico() {
        Order guardada = new Order();
        guardada.setOrderId(1);
        when(orderRepository.save(any(), eq(USER), eq(2))).thenReturn(guardada);

        Order resultado = service.save(ordenNueva(null), USER);

        assertEquals(1, resultado.getOrderId());
        verify(orderRepository, never()).findByClientRef(anyString());
    }

    @Test
    void clientRefEnBlancoNoConsultaIdempotencia() {
        Order guardada = new Order();
        guardada.setOrderId(2);
        when(orderRepository.save(any(), eq(USER), eq(2))).thenReturn(guardada);

        service.save(ordenNueva("  "), USER);

        verify(orderRepository, never()).findByClientRef(anyString());
    }

    @Test
    void elUpdateNoPasaPorElLookupDeIdempotencia() {
        Order edicion = ordenNueva(REF);
        edicion.setOrderId(500);
        when(orderRepository.getOrderUser(500, USER)).thenReturn(Optional.of(edicion));
        when(orderRepository.save(any(), eq(USER), eq(2))).thenReturn(edicion);

        service.save(edicion, USER);

        verify(orderRepository, never()).findByClientRef(anyString());
    }
}

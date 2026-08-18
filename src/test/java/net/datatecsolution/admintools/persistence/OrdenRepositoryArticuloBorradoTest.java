package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Order;
import net.datatecsolution.admintools.persistence.crud.OrdenCRUD;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.entity.Articulo;
import net.datatecsolution.admintools.persistence.entity.DetalleOrden;
import net.datatecsolution.admintools.persistence.entity.Orden;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import net.datatecsolution.admintools.persistence.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * US-147 — una orden cuya línea referencia un artículo BORRADO del catálogo
 * (la relación a articulo_view hidrata null) no debe tumbar la lista.
 *
 * Caso real (Mariposas, 2026-08-18): la orden 79 tenía el artículo 176,
 * borrado después de guardarla. aplicarPreciosUsuario hacía
 * getArticulo().setPrecioArticulos(...) → NPE → 500 en /orders/pending →
 * el POS mostraba la lista VACÍA aunque había 18 órdenes sanas.
 */
@ExtendWith(MockitoExtension.class)
class OrdenRepositoryArticuloBorradoTest {

    @Mock private OrdenCRUD ordenCRUD;
    @Mock private PreciosArticuloCRUD preciosArticuloCRUD;
    @Mock private OrderMapper mapper;

    @InjectMocks private OrdenRepository repository;

    private static DetalleOrden linea(Articulo articulo) {
        DetalleOrden d = new DetalleOrden();
        d.setArticulo(articulo);
        return d;
    }

    private static Orden orden(DetalleOrden... detalles) {
        Orden o = new Orden();
        o.setDetalles(List.of(detalles));
        return o;
    }

    @Test
    void lineaConArticuloBorrado_noRevientaLaLista() {
        Articulo sano = new Articulo();
        // una orden mixta: línea sana + línea huérfana (artículo borrado)
        Orden conHuerfana = orden(linea(sano), linea(null));
        Page<Orden> page = new PageImpl<>(List.of(conHuerfana));
        when(ordenCRUD.findPendientesVisibles(anyString(), anyInt(), any(Pageable.class)))
                .thenReturn(page);
        lenient().when(preciosArticuloCRUD.findPrecioUser(anyInt(), anyString()))
                .thenReturn(List.of(new PrecioArticulo()));
        when(mapper.toOrder(any(Orden.class))).thenReturn(new Order());

        assertThatCode(() -> repository.getPendientes("tecnico", PageRequest.of(0, 50)))
                .doesNotThrowAnyException();
    }

    @Test
    void lineaSana_sigueRecibiendoPreciosDeUsuario() {
        Articulo sano = new Articulo();
        Orden o = orden(linea(sano));
        when(ordenCRUD.findPendientesVisibles(anyString(), anyInt(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o)));
        List<PrecioArticulo> precios = List.of(new PrecioArticulo());
        when(preciosArticuloCRUD.findPrecioUser(anyInt(), eq("tecnico"))).thenReturn(precios);
        when(mapper.toOrder(any(Orden.class))).thenReturn(new Order());

        repository.getPendientes("tecnico", PageRequest.of(0, 50));

        assertThat(sano.getPrecioArticulos()).isSameAs(precios);
    }
}

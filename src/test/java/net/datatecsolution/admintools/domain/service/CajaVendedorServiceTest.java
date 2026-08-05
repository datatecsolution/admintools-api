package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * US-130 — resolución de caja/bodega compartida entre el guard del save y la
 * búsqueda de productos.
 *
 * El bug que motiva esto: la búsqueda mostraba stock de articulo_view (bodega
 * 1 cableada) mientras el guard validaba en la bodega del vendedor — el
 * vendedor veía 452 y el guardado le decía "hay 0 disponibles" (AGUA
 * OXIGENADA, Sharon 2026-08-03). Ambos caminos deben mirar la misma bodega.
 */
class CajaVendedorServiceTest {

    @Mock
    private CajaUsuarioCRUD cajaUsuarioCRUD;

    @InjectMocks
    private CajaVendedorService servicio;

    private static final String USER = "ELMER22GUTI";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static Object[] fila(int caja, Integer bodega) {
        return new Object[]{caja, bodega};
    }

    @Test
    void resuelveDesdeCajasUsuarios() {
        when(cajaUsuarioCRUD.findCajaEfectiva(USER))
                .thenReturn(List.<Object[]>of(fila(3, 2)));

        Optional<CajaVendedorService.CajaVendedor> caja = servicio.resolver(USER);

        assertTrue(caja.isPresent());
        assertEquals(3, caja.get().codigo());
        assertEquals(2, caja.get().bodega());
    }

    @Test
    void caeAlLegacyCuandoNoHayFilaEnCajasUsuarios() {
        when(cajaUsuarioCRUD.findCajaEfectiva(USER)).thenReturn(List.of());
        when(cajaUsuarioCRUD.findCajaLegacy(USER))
                .thenReturn(List.<Object[]>of(fila(2, 1)));

        Optional<CajaVendedorService.CajaVendedor> caja = servicio.resolver(USER);

        assertTrue(caja.isPresent());
        assertEquals(2, caja.get().codigo());
        assertEquals(1, caja.get().bodega());
    }

    @Test
    void sinCajaPorNingunaViaDevuelveEmpty() {
        // El caller decide el error (el save responde 409) — nunca caer en
        // silencio a la caja 1, que era el bug del DEFAULT.
        when(cajaUsuarioCRUD.findCajaEfectiva(USER)).thenReturn(List.of());
        when(cajaUsuarioCRUD.findCajaLegacy(USER)).thenReturn(List.of());

        assertTrue(servicio.resolver(USER).isEmpty());
    }

    @Test
    void bodegaNulaDeLaCajaSeNormalizaA1() {
        when(cajaUsuarioCRUD.findCajaEfectiva(USER))
                .thenReturn(List.<Object[]>of(fila(5, null)));

        assertEquals(1, servicio.resolver(USER).orElseThrow().bodega());
    }

    /* ===== bodegaParaBusqueda: navegar nunca se rompe ===== */

    @Test
    void laBusquedaUsaLaBodegaDelVendedor() {
        when(cajaUsuarioCRUD.findCajaEfectiva(USER))
                .thenReturn(List.<Object[]>of(fila(3, 2)));

        assertEquals(2, servicio.bodegaParaBusqueda(USER));
    }

    @Test
    void laBusquedaSinCajaCaeABodega1() {
        // Sin caja, el catalogo se sigue pudiendo NAVEGAR (comportamiento
        // historico de articulo_view); el que frena es el 409 del save.
        when(cajaUsuarioCRUD.findCajaEfectiva(USER)).thenReturn(List.of());
        when(cajaUsuarioCRUD.findCajaLegacy(USER)).thenReturn(List.of());

        assertEquals(1, servicio.bodegaParaBusqueda(USER));
    }
}

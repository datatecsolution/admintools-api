package net.datatecsolution.admintools.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FacturaTest {

    @Test
    void calcularTotales_noModificaLaFechaExistente() {
        // Regresion: antes calcularTotales() reseteaba fecha=now() en cada
        // save, lo que migraba ordenes editadas al dia actual y rompia el
        // filtro getByToday. Ahora la fecha solo se setea en @PrePersist.
        Factura f = new Factura();
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 1, 15, 9, 30);
        f.setFecha(fechaOriginal);
        f.setDetalles(Collections.emptyList());

        f.calcularTotales();

        assertEquals(fechaOriginal, f.getFecha(),
                "calcularTotales no debe modificar la fecha de creacion");
    }

    @Test
    void calcularTotales_noEstableceFechaSiVieneNula() {
        // Caso de orden recien instanciada antes de pasar por JPA. El
        // @PrePersist sera quien setee la fecha en el INSERT — el metodo
        // no debe adelantarse.
        Factura f = new Factura();
        f.setDetalles(Collections.emptyList());

        f.calcularTotales();

        assertNull(f.getFecha(),
                "calcularTotales no debe inicializar fecha; eso es trabajo del @PrePersist");
    }
}

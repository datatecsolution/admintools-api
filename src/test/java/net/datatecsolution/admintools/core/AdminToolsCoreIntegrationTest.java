package net.datatecsolution.admintools.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke test de US-016: confirma que el modulo admintools-core esta
 * accesible como dependencia desde admintools-api.
 *
 * Si esta clase no compila → la dependencia no se resolvio (revisar
 * mavenLocal y que el JAR este publicado).
 * Si el test falla → la logica del modulo cambio inesperadamente.
 */
class AdminToolsCoreIntegrationTest {

    @Test
    void calcularDescuentoPorcentaje_consume_logica_compartida_desde_core() {
        // 2 unidades * 100.00 precio * 15% = 30.00, redondeado a 0 decimales
        BigDecimal descuento = FacturacionCalculadora.calcularDescuentoPorcentaje(
                new BigDecimal("2"),
                100.00,
                15.00
        );

        assertEquals(0, descuento.compareTo(new BigDecimal("30")),
                "FacturacionCalculadora debe estar disponible desde admintools-core y aplicar 15% sobre 200 = 30");
    }
}

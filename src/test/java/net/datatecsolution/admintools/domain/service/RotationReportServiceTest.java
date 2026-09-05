package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.RotationClassSummary;
import net.datatecsolution.admintools.domain.dto.RotationItem;
import net.datatecsolution.admintools.domain.dto.RotationReportResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * US-063 — rotación de inventario. Se mockea la query cross-DB y se verifica
 * el cálculo de venta diaria / cobertura / rotación y la clasificación por
 * días de cobertura, incluidos los casos borde (sin movimiento, sin stock).
 */
@ExtendWith(MockitoExtension.class)
class RotationReportServiceTest {

    @Mock private DataSource commonDS;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private JdbcTemplate jdbc;

    private RotationReportService service;

    // 30 días exactos: FROM..TO inclusive
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 30);

    @BeforeEach
    void setUp() {
        service = new RotationReportService(commonDS, cajaCRUD);
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
    }

    private Caja caja(int codigo, String db) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setDescripcion("Caja " + codigo);
        c.setNombreDb(db);
        return c;
    }

    private void unaCaja() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
    }

    private Map<String, Object> fila(int codigo, String nombre, String unidades, String venta, String stock) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigo_articulo", codigo);
        m.put("articulo", nombre);
        m.put("codigo_marca", 1);
        m.put("categoria", "General");
        m.put("unidades", new BigDecimal(unidades));
        m.put("venta", new BigDecimal(venta));
        m.put("stock", new BigDecimal(stock));
        return m;
    }

    @Test
    void calculaCoberturaYClasificaPorDias() {
        unaCaja();
        // 30 días. Umbral rápido 30, medio 90.
        // rapido: 60 uds/30d = 2/día, stock 20 → cobertura 10d → RAPIDO
        // medio:  30 uds/30d = 1/día, stock 60 → cobertura 60d → MEDIO
        // lento:  3 uds/30d = 0.1/día, stock 50 → cobertura 500d → LENTO
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(1, "Vuela", "60", "600.00", "20"),
                fila(2, "Camina", "30", "300.00", "60"),
                fila(3, "Duerme", "3", "30.00", "50")));

        RotationReportResponse r = service.rotation(FROM, TO, null, null, null, 30, 90, null);

        assertThat(r.dias()).isEqualTo(30);
        assertThat(r.items()).extracting(RotationItem::clasificacion)
                .containsExactly("RAPIDO", "MEDIO", "LENTO");
        RotationItem vuela = r.items().get(0);
        assertThat(vuela.ventaDiaria()).isEqualByComparingTo("2");
        assertThat(vuela.diasCobertura()).isEqualByComparingTo("10.0");
        assertThat(vuela.rotacion()).isEqualByComparingTo("3.00"); // 60/20
        assertThat(vuela.sinMovimiento()).isFalse();
    }

    @Test
    void sinMovimientoEsLentoConCoberturaNula() {
        unaCaja();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(9, "Muerto", "0", "0.00", "40")));

        RotationReportResponse r = service.rotation(FROM, TO, null, null, null, 30, 90, null);

        RotationItem it = r.items().get(0);
        assertThat(it.clasificacion()).isEqualTo("LENTO");
        assertThat(it.sinMovimiento()).isTrue();
        assertThat(it.diasCobertura()).isNull();
        assertThat(it.rotacion()).isEqualByComparingTo("0.00"); // 0/40
    }

    @Test
    void conVentasYSinStockEsRapidoConRotacionNula() {
        unaCaja();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(7, "Agotado", "15", "150.00", "0")));

        RotationReportResponse r = service.rotation(FROM, TO, null, null, null, 30, 90, null);

        RotationItem it = r.items().get(0);
        assertThat(it.clasificacion()).isEqualTo("RAPIDO"); // cobertura 0 días
        assertThat(it.diasCobertura()).isEqualByComparingTo("0.0");
        assertThat(it.rotacion()).isNull();
    }

    @Test
    void resumenCuentaSinMovimientoYNoSeRecortaConLimit() {
        unaCaja();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(1, "Vuela", "60", "600.00", "20"),
                fila(9, "Muerto", "0", "0.00", "40")));

        RotationReportResponse r = service.rotation(FROM, TO, null, null, null, 30, 90, 1);

        assertThat(r.items()).hasSize(1);
        assertThat(r.resumen()).extracting(RotationClassSummary::clasificacion)
                .containsExactly("RAPIDO", "MEDIO", "LENTO");
        RotationClassSummary lento = r.resumen().get(2);
        assertThat(lento.productos()).isEqualTo(1);
        assertThat(lento.sinMovimiento()).isEqualTo(1);
        assertThat(lento.stockActual()).isEqualByComparingTo("40");
    }

    @Test
    void bodegaYExclusionAgreganArgsEnOrdenPosicional() {
        unaCaja();
        List<Object> capturedArgs = new ArrayList<>();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            assertThat(sql).contains("existencia_articulo_bodega")
                           .contains("codigo_bodega = ?")
                           .contains("NOT IN (?)")
                           .contains("a.estado = 1");
            Object[] all = inv.getArguments();
            for (int i = 1; i < all.length; i++) capturedArgs.add(all[i]);
            return List.of();
        });

        service.rotation(FROM, TO, null, 2, List.of("TECNO"), 30, 90, null);

        // (from, to) de la caja + bodega + categoría excluida, en ese orden
        assertThat(capturedArgs).hasSize(4);
        assertThat(capturedArgs.get(2)).isEqualTo(2);
        assertThat(capturedArgs.get(3)).isEqualTo("TECNO");
    }

    @Test
    void umbralesInvalidosRechazan400() {
        assertThatThrownBy(() -> service.rotation(FROM, TO, null, null, null, 90, 30, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("umbralRapidoDias");
    }

    @Test
    void rangoInvertidoRechaza400() {
        assertThatThrownBy(() -> service.rotation(TO, FROM, null, null, null, 30, 90, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rango");
    }
}

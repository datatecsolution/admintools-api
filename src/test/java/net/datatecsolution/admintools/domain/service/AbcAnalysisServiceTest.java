package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.AbcClassSummary;
import net.datatecsolution.admintools.domain.dto.AbcItem;
import net.datatecsolution.admintools.domain.dto.AbcReportResponse;
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
 * US-062 — análisis ABC. La clasificación (participación acumulada + cortes)
 * es lógica pura; se mockea el acceso cross-DB (queryForList) y el catálogo
 * de cajas, y se verifica la clasificación con montos conocidos.
 */
@ExtendWith(MockitoExtension.class)
class AbcAnalysisServiceTest {

    @Mock private DataSource commonDS;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private JdbcTemplate jdbc;

    private AbcAnalysisService service;

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 31);
    private static final BigDecimal A80 = new BigDecimal("80");
    private static final BigDecimal B95 = new BigDecimal("95");

    @BeforeEach
    void setUp() {
        service = new AbcAnalysisService(commonDS, cajaCRUD);
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
    }

    private Caja caja(int codigo, String db) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setDescripcion("Caja " + codigo);
        c.setNombreDb(db);
        return c;
    }

    private Map<String, Object> fila(int codigo, String nombre, String categoria,
                                     String unidades, String venta) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigo_articulo", codigo);
        m.put("articulo", nombre);
        m.put("codigo_marca", categoria == null ? null : 1);
        m.put("categoria", categoria);
        m.put("unidades", new BigDecimal(unidades));
        m.put("venta", new BigDecimal(venta));
        return m;
    }

    @Test
    void clasificaAbcPorParticipacionAcumulada() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        // 4 productos, venta total 1000. Clase por acumulado ANTERIOR:
        // 700 (antes 0%) → A, 200 (antes 70%) → A, 60 (antes 90%) → B,
        // 40 (antes 96%) → C.
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(10, "Estrella", "Panadería", "70", "700.00"),
                fila(20, "Segundo", "Panadería", "20", "200.00"),
                fila(30, "Cola1", "Cafetería", "6", "60.00"),
                fila(40, "Cola2", "Cafetería", "4", "40.00")));

        AbcReportResponse r = service.abc(FROM, TO, null, null, A80, B95, null);

        assertThat(r.totalVenta()).isEqualByComparingTo("1000.00");
        assertThat(r.items()).extracting(AbcItem::clase).containsExactly("A", "A", "B", "C");
        assertThat(r.items().get(0).participacion()).isEqualByComparingTo("70.00");
        assertThat(r.items().get(1).acumulado()).isEqualByComparingTo("90.00");
        assertThat(r.items().get(3).acumulado()).isEqualByComparingTo("100.00");

        // resumen por clase (sobre la lista completa)
        assertThat(r.resumen()).extracting(AbcClassSummary::clase).containsExactly("A", "B", "C");
        assertThat(r.resumen().get(0).venta()).isEqualByComparingTo("900.00");
        assertThat(r.resumen().get(2).productos()).isEqualTo(1);
        assertThat(r.resumen().get(2).participacion()).isEqualByComparingTo("4.00");
    }

    @Test
    void consolidadoRepiteLosArgsDeFechaPorCaja() {
        when(cajaCRUD.findAll()).thenReturn(List.of(
                caja(1, "admin_tools_caja_1"), caja(2, "admin_tools_caja_2")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(2)).thenReturn(Optional.of(caja(2, "admin_tools_caja_2")));

        List<Object> capturedArgs = new ArrayList<>();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            assertThat(sql).contains("admin_tools_caja_1.detalle_factura")
                           .contains("admin_tools_caja_2.detalle_factura")
                           .contains("UNION ALL");
            // queryForList(String, Object...) es varargs: los args vienen
            // expandidos a partir del índice 1
            Object[] all = inv.getArguments();
            for (int i = 1; i < all.length; i++) capturedArgs.add(all[i]);
            return List.of();
        });

        service.abc(FROM, TO, null, null, A80, B95, null);

        // 2 cajas × (from, to) = 4 args posicionales
        assertThat(capturedArgs).hasSize(4);
    }

    @Test
    void excluyeCategoriasPorParametro() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));

        List<Object> capturedArgs = new ArrayList<>();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            assertThat(sql).contains("NOT IN (?)");
            Object[] all = inv.getArguments();
            for (int i = 1; i < all.length; i++) capturedArgs.add(all[i]);
            return List.of();
        });

        service.abc(FROM, TO, null, List.of("TECNO"), A80, B95, null);

        assertThat(capturedArgs).contains("TECNO");
    }

    @Test
    void limitRecortaItemsPeroNoElResumen() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(10, "P1", "X", "1", "700.00"),
                fila(20, "P2", "X", "1", "200.00"),
                fila(30, "P3", "X", "1", "100.00")));

        AbcReportResponse r = service.abc(FROM, TO, null, null, A80, B95, 1);

        assertThat(r.items()).hasSize(1);
        assertThat(r.resumen().stream().mapToInt(AbcClassSummary::productos).sum()).isEqualTo(3);
    }

    @Test
    void articuloBorradoDelCatalogoNoDesapareceDeLaVenta() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        Map<String, Object> huerfano = new HashMap<>();
        huerfano.put("codigo_articulo", 99);
        huerfano.put("articulo", null);
        huerfano.put("codigo_marca", null);
        huerfano.put("categoria", null);
        huerfano.put("unidades", new BigDecimal("5"));
        huerfano.put("venta", new BigDecimal("100.00"));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(huerfano));

        AbcReportResponse r = service.abc(FROM, TO, null, null, A80, B95, null);

        assertThat(r.items()).hasSize(1);
        assertThat(r.items().get(0).articulo()).isEqualTo("(artículo eliminado)");
        assertThat(r.items().get(0).clase()).isEqualTo("A");
    }

    @Test
    void sinVentasDevuelveVacioConTotalCero() {
        when(cajaCRUD.findAll()).thenReturn(List.of(caja(1, "admin_tools_caja_1")));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        AbcReportResponse r = service.abc(FROM, TO, null, null, A80, B95, null);

        assertThat(r.totalVenta()).isEqualByComparingTo("0");
        assertThat(r.items()).isEmpty();
    }

    @Test
    void umbralesInvalidosRechazan400() {
        assertThatThrownBy(() -> service.abc(FROM, TO, null, null, B95, A80, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("umbralA");
    }

    @Test
    void cajaInexistenteDa404() {
        when(cajaCRUD.findById(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.abc(FROM, TO, 9, null, A80, B95, null))
                .isInstanceOf(ResponseStatusException.class);
    }
}

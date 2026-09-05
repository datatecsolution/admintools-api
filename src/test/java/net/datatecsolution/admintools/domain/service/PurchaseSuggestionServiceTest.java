package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionItem;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionsResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseUrgencySummary;
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
 * US-064 — proyección de compras. Se mockea la query cross-DB y se verifica
 * la fórmula del sugerido (ceiling a enteros), las urgencias y el orden.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseSuggestionServiceTest {

    @Mock private DataSource commonDS;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private JdbcTemplate jdbc;

    private PurchaseSuggestionService service;

    // 30 días exactos
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 30);

    @BeforeEach
    void setUp() {
        service = new PurchaseSuggestionService(commonDS, cajaCRUD);
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
    }

    private void unaCaja() {
        Caja c = new Caja();
        c.setCodigo(1);
        c.setDescripcion("Caja 1");
        c.setNombreDb("admin_tools_caja_1");
        when(cajaCRUD.findAll()).thenReturn(List.of(c));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(c));
    }

    private Map<String, Object> fila(int codigo, String nombre, String unidades, String stock, String minimo) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigo_articulo", codigo);
        m.put("articulo", nombre);
        m.put("codigo_marca", 1);
        m.put("categoria", "General");
        m.put("unidades", new BigDecimal(unidades));
        m.put("stock", new BigDecimal(stock));
        m.put("minimo", new BigDecimal(minimo));
        return m;
    }

    @Test
    void calculaSugeridoConCeilingYUrgencias() {
        unaCaja();
        // 30 días, horizonte 30:
        // critica: 60 uds (2/día), stock 5, minimo 10 → 5<=10 CRITICA;
        //          sugerido = 2*30+10-5 = 65
        // alta:    30 uds (1/día), stock 6, minimo 0 → cobertura 6d → ALTA;
        //          sugerido = 30+0-6 = 24
        // normal:  15 uds (0.5/día), stock 10, minimo 0 → cobertura 20d → NORMAL;
        //          sugerido = 15+0-10 = 5
        // fraccion: 10 uds (0.3333/día), stock 0, min 0 → agotado con venta → CRITICA;
        //          sugerido = 0.3333*30 = 9.999 → CEILING → 10
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(1, "Critico", "60", "5", "10"),
                fila(2, "Alto", "30", "6", "0"),
                fila(3, "Normal", "15", "10", "0"),
                fila(4, "Fraccion", "10", "0", "0")));

        PurchaseSuggestionsResponse r = service.suggestions(FROM, TO, null, null, null, 30, false, null);

        // orden: CRITICA (cobertura 0 primero, luego 2.5d), ALTA, NORMAL
        assertThat(r.items()).extracting(PurchaseSuggestionItem::articulo)
                .containsExactly("Fraccion", "Critico", "Alto", "Normal");
        Map<String, PurchaseSuggestionItem> por = new HashMap<>();
        r.items().forEach(i -> por.put(i.articulo(), i));
        assertThat(por.get("Critico").sugerido()).isEqualByComparingTo("65");
        assertThat(por.get("Critico").urgencia()).isEqualTo("CRITICA");
        assertThat(por.get("Alto").sugerido()).isEqualByComparingTo("24");
        assertThat(por.get("Alto").urgencia()).isEqualTo("ALTA");
        assertThat(por.get("Normal").sugerido()).isEqualByComparingTo("5");
        assertThat(por.get("Normal").urgencia()).isEqualTo("NORMAL");
        assertThat(por.get("Fraccion").sugerido()).isEqualByComparingTo("10");
        assertThat(por.get("Fraccion").urgencia()).isEqualTo("CRITICA");

        assertThat(r.resumen()).extracting(PurchaseUrgencySummary::urgencia)
                .containsExactly("CRITICA", "ALTA", "NORMAL");
        assertThat(r.resumen().get(0).productos()).isEqualTo(2);
    }

    @Test
    void productoConStockSuficienteQuedaFueraSalvoIncluirTodos() {
        unaCaja();
        // 30 uds (1/día), stock 100, minimo 0 → sugerido 0 → fuera del default
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(5, "Sobrado", "30", "100", "0")));

        PurchaseSuggestionsResponse sinTodos = service.suggestions(FROM, TO, null, null, null, 30, false, null);
        assertThat(sinTodos.items()).isEmpty();

        PurchaseSuggestionsResponse conTodos = service.suggestions(FROM, TO, null, null, null, 30, true, null);
        assertThat(conTodos.items()).hasSize(1);
        assertThat(conTodos.items().get(0).sugerido()).isEqualByComparingTo("0");
    }

    @Test
    void sinVentasConMinimoSugiereReponerElMinimo() {
        unaCaja();
        // 0 uds, stock 2, minimo 10 → bajo mínimo → CRITICA; sugerido = 0+10-2 = 8
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(6, "Reserva", "0", "2", "10")));

        PurchaseSuggestionsResponse r = service.suggestions(FROM, TO, null, null, null, 30, false, null);

        PurchaseSuggestionItem it = r.items().get(0);
        assertThat(it.sugerido()).isEqualByComparingTo("8");
        assertThat(it.urgencia()).isEqualTo("CRITICA");
        assertThat(it.diasCobertura()).isNull();
    }

    @Test
    void bodegaAplicaAStockYMinimoEnOrdenPosicional() {
        unaCaja();
        List<Object> capturedArgs = new ArrayList<>();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            assertThat(sql).contains("existencia_articulo_bodega")
                           .contains("articulo_kardex");
            assertThat(sql.split("codigo_bodega = \\?")).hasSize(3); // 2 ocurrencias
            Object[] all = inv.getArguments();
            for (int i = 1; i < all.length; i++) capturedArgs.add(all[i]);
            return List.of();
        });

        service.suggestions(FROM, TO, null, 2, List.of("TECNO"), 30, false, null);

        // (from,to) + bodega stock + bodega minimo + categoria
        assertThat(capturedArgs).hasSize(5);
        assertThat(capturedArgs.get(2)).isEqualTo(2);
        assertThat(capturedArgs.get(3)).isEqualTo(2);
        assertThat(capturedArgs.get(4)).isEqualTo("TECNO");
    }

    @Test
    void limitRecortaItemsPeroNoElResumen() {
        unaCaja();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                fila(1, "Critico", "60", "5", "10"),
                fila(2, "Alto", "30", "6", "0")));

        PurchaseSuggestionsResponse r = service.suggestions(FROM, TO, null, null, null, 30, false, 1);

        assertThat(r.items()).hasSize(1);
        assertThat(r.resumen().stream().mapToInt(PurchaseUrgencySummary::productos).sum()).isEqualTo(2);
    }

    @Test
    void horizonteInvalidoRechaza400() {
        assertThatThrownBy(() -> service.suggestions(FROM, TO, null, null, null, 0, false, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("horizonteDias");
    }
}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CategoryProjection;
import net.datatecsolution.admintools.domain.dto.CategorySalesResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * US-106 — comparativo trimestral + proyección. Se mockean las dos queries
 * (la trimestral y la YTD del año anterior, distinguibles porque solo la
 * trimestral trae YEAR/QUARTER en el SQL) y se verifica cada método de
 * proyección con montos conocidos.
 */
@ExtendWith(MockitoExtension.class)
class CategorySalesServiceTest {

    @Mock private DataSource commonDS;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private JdbcTemplate jdbc;

    private CategorySalesService service;

    // "hoy" = 5-sep-2026 → T3 en curso (jul-sep: 92 días, 67 transcurridos)
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 5);

    @BeforeEach
    void setUp() {
        service = new CategorySalesService(commonDS, cajaCRUD);
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        Caja c = new Caja();
        c.setCodigo(1);
        c.setDescripcion("Caja 1");
        c.setNombreDb("admin_tools_caja_1");
        when(cajaCRUD.findAll()).thenReturn(List.of(c));
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(c));
    }

    private Map<String, Object> filaTri(int anio, int tri, String cat, String unid, String venta) {
        Map<String, Object> m = new HashMap<>();
        m.put("anio", anio);
        m.put("trimestre", tri);
        m.put("codigo_marca", 1);
        m.put("categoria", cat);
        m.put("unidades", new BigDecimal(unid));
        m.put("venta", new BigDecimal(venta));
        return m;
    }

    private Map<String, Object> filaYtd(String cat, String unid, String venta) {
        Map<String, Object> m = new HashMap<>();
        m.put("categoria", cat);
        m.put("unidades", new BigDecimal(unid));
        m.put("venta", new BigDecimal(venta));
        return m;
    }

    private void mockQueries(List<Map<String, Object>> tri, List<Map<String, Object>> ytdPrev) {
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            return sql.contains("QUARTER(") ? tri : ytdPrev;
        });
    }

    private CategoryProjection proy(CategorySalesResponse r, int tri) {
        return r.proyecciones().stream()
                .filter(p -> p.trimestre() == tri)
                .findFirst().orElse(null);
    }

    @Test
    void factorYoyProyectaSobreElTrimestreDelAnioAnterior() {
        // 2025: T3=1000, T4=2000. 2026 YTD (T1+T2+parcial T3) = 1200;
        // YTD 2025 al mismo día = 1000 → factor 1.2.
        mockQueries(List.of(
                filaTri(2025, 3, "Pan", "100", "1000.00"),
                filaTri(2025, 4, "Pan", "200", "2000.00"),
                filaTri(2026, 1, "Pan", "50", "500.00"),
                filaTri(2026, 2, "Pan", "40", "400.00"),
                filaTri(2026, 3, "Pan", "30", "300.00")),
                List.of(filaYtd("Pan", "100", "1000.00")));

        CategorySalesResponse r = service.categorySales(2025, HASTA, null, null);

        CategoryProjection t3 = proy(r, 3);
        assertThat(t3.metodo()).isEqualTo("FACTOR_YOY");
        assertThat(t3.venta()).isEqualByComparingTo("1200.00"); // 1000 × 1.2
        CategoryProjection t4 = proy(r, 4);
        assertThat(t4.metodo()).isEqualTo("FACTOR_YOY");
        assertThat(t4.venta()).isEqualByComparingTo("2400.00"); // 2000 × 1.2
    }

    @Test
    void sinBaseInteranualUsaRunRateSoloParaElTrimestreEnCurso() {
        // Categoría nueva: solo parcial de T3-2026 (300 en 67 de 92 días).
        mockQueries(List.of(filaTri(2026, 3, "Nueva", "30", "300.00")), List.of());

        CategorySalesResponse r = service.categorySales(2025, HASTA, null, null);

        CategoryProjection t3 = proy(r, 3);
        assertThat(t3.metodo()).isEqualTo("RUN_RATE");
        assertThat(t3.venta()).isEqualByComparingTo("411.94"); // 300 × 92/67
        assertThat(proy(r, 4)).isNull(); // sin base para T4: no se inventa
    }

    @Test
    void sinFactorPeroConTemporadaAnteriorRepiteLaTemporada() {
        // Hay T4-2025 pero el YTD anterior es 0 (abrió en T4) → factor null;
        // T3 sale por run-rate y T4 repite la temporada del año pasado.
        mockQueries(List.of(
                filaTri(2025, 4, "Estacional", "500", "5000.00"),
                filaTri(2026, 3, "Estacional", "30", "300.00")),
                List.of());

        CategorySalesResponse r = service.categorySales(2025, HASTA, null, null);

        assertThat(proy(r, 3).metodo()).isEqualTo("RUN_RATE");
        CategoryProjection t4 = proy(r, 4);
        assertThat(t4.metodo()).isEqualTo("ANIO_ANTERIOR");
        assertThat(t4.venta()).isEqualByComparingTo("5000.00");
    }

    @Test
    void lasFilasCrudasSeDevuelvenTalCual() {
        mockQueries(List.of(
                filaTri(2025, 1, "Pan", "10", "100.00"),
                filaTri(2026, 1, "Pan", "20", "200.00")),
                List.of());

        CategorySalesResponse r = service.categorySales(2025, HASTA, null, null);

        assertThat(r.filas()).hasSize(2);
        assertThat(r.filas().get(0).anio()).isEqualTo(2025);
        assertThat(r.hasta()).isEqualTo(HASTA);
    }

    @Test
    void exclusionDeCategoriasViajaComoParametro() {
        List<Object> captured = new ArrayList<>();
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            assertThat(sql).contains("NOT IN (?)");
            Object[] all = inv.getArguments();
            for (int i = 1; i < all.length; i++) captured.add(all[i]);
            return List.<Map<String, Object>>of();
        });

        service.categorySales(2025, HASTA, null, List.of("TECNO"));

        assertThat(captured).contains("TECNO");
    }
}

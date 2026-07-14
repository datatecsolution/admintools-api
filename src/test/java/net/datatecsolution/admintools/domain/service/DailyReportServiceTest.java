package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CajaDailyReport;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * US-047 — reporte diario. La consolidación es lógica pura; lo único que se
 * mockea es el acceso cross-DB (JdbcTemplate.queryForMap por caja) y el
 * catálogo de cajas. Se verifica que el consolidado suma correctamente todas
 * las columnas de dos cajas con montos conocidos.
 */
@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

    @Mock private DataSource commonDS;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private JdbcTemplate jdbc;

    private DailyReportService service;

    @BeforeEach
    void setUp() {
        service = new DailyReportService(commonDS, cajaCRUD);
        // el service crea su propio JdbcTemplate en el constructor; lo suplantamos
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
    }

    private Caja caja(int codigo, String db) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setDescripcion("Caja " + codigo);
        c.setNombreDb(db);
        return c;
    }

    private Map<String, Object> actRow(long ventas, String total, String efectivo, String tarjeta,
                                       String credito, String descuentos) {
        Map<String, Object> m = new HashMap<>();
        m.put("ventas", ventas);
        m.put("total", new BigDecimal(total));
        m.put("efectivo", new BigDecimal(efectivo));
        m.put("tarjeta", new BigDecimal(tarjeta));
        m.put("credito", new BigDecimal(credito));
        m.put("descuentos", new BigDecimal(descuentos));
        m.put("exento", BigDecimal.ZERO);
        m.put("base15", BigDecimal.ZERO);
        m.put("base18", BigDecimal.ZERO);
        m.put("isv15", BigDecimal.ZERO);
        m.put("isv18", BigDecimal.ZERO);
        return m;
    }

    private Map<String, Object> nulasRow(long anuladas, String totalAnulado) {
        Map<String, Object> m = new HashMap<>();
        m.put("anuladas", anuladas);
        m.put("total_anulado", new BigDecimal(totalAnulado));
        return m;
    }

    @Test
    void daily_consolidaSumaDeDosCajas() {
        when(cajaCRUD.findAll()).thenReturn(List.of(
                caja(1, "admin_tools_caja_1"), caja(2, "admin_tools_caja_2")));

        // el consolidado no depende de qué caja: sumamos ambas. Diferenciamos
        // por el nombre de BD embebido en el SQL y por ACT vs no-ACT.
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            boolean caja1 = sql.contains("admin_tools_caja_1");
            boolean act = sql.contains("estado_factura = 'ACT'");
            if (act) {
                return caja1
                        ? actRow(10, "1000.00", "600.00", "300.00", "100.00", "50.00")
                        : actRow(5, "500.00", "200.00", "100.00", "200.00", "25.00");
            }
            return caja1 ? nulasRow(2, "200.00") : nulasRow(1, "100.00");
        });

        DailyReportResponse r = service.daily(LocalDate.of(2026, 7, 10), null);

        assertThat(r.cajas()).hasSize(2);
        CajaDailyReport cons = r.consolidado();
        assertThat(cons.caja()).isZero();
        assertThat(cons.nombreCaja()).isEqualTo("CONSOLIDADO");
        assertThat(cons.ventas()).isEqualTo(15);
        assertThat(cons.total()).isEqualByComparingTo("1500.00");
        assertThat(cons.efectivo()).isEqualByComparingTo("800.00");
        assertThat(cons.tarjeta()).isEqualByComparingTo("400.00");
        assertThat(cons.credito()).isEqualByComparingTo("300.00");
        assertThat(cons.descuentos()).isEqualByComparingTo("75.00");
        assertThat(cons.anuladas()).isEqualTo(3);
        assertThat(cons.totalAnulado()).isEqualByComparingTo("300.00");
    }
}

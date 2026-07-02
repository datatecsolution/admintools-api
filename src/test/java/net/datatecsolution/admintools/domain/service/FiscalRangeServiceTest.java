package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.FiscalRangeRequest;
import net.datatecsolution.admintools.domain.dto.FiscalRangeResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-101 — guardas del Swing sobre datos_factura:
 * verificarFacturacionFactInicial (409), final &lt; inicial (422),
 * verificarFacturacionEliminacion (409) y setNumeroFact (ALTER AUTO_INCREMENT).
 */
@ExtendWith(MockitoExtension.class)
class FiscalRangeServiceTest {

    private static final String DB = "admin_tools_caja_2";

    @Mock private JdbcTemplate jdbc;
    @Mock private CajaCRUD cajaCRUD;

    private FiscalRangeService service() {
        Caja caja = new Caja();
        caja.setCodigo(2);
        caja.setNombreDb(DB);
        lenient().when(cajaCRUD.findById(2)).thenReturn(Optional.of(caja));
        return new FiscalRangeService(jdbc, cajaCRUD);
    }

    private FiscalRangeRequest request(int inicial, int fin) {
        return new FiscalRangeRequest("CAI-123", inicial, fin, "000-001-01-",
                fin - inicial + 1, LocalDate.of(2027, 1, 31), null);
    }

    private FiscalRangeResponse row(int id, boolean enUso) {
        return new FiscalRangeResponse(id, "CAI-123", 1001, 2000, "000-001-01-",
                1000, LocalDate.of(2027, 1, 31), "", enUso);
    }

    @Test
    void create_inicialMenorAlUltimoEmitido_409() {
        FiscalRangeService svc = service();
        when(jdbc.queryForObject(contains("MAX(numero_factura)"), eq(Integer.class))).thenReturn(500);

        assertThatThrownBy(() -> svc.create(2, request(500, 1500)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void create_finalMenorAlInicial_422() {
        FiscalRangeService svc = service();

        assertThatThrownBy(() -> svc.create(2, request(1000, 999)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void create_ok_insertaYReposicionaAutoIncrement() {
        FiscalRangeService svc = service();
        when(jdbc.queryForObject(contains("MAX(numero_factura)"), eq(Integer.class))).thenReturn(1000);
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(inv -> {
            KeyHolder kh = inv.getArgument(1);
            Map<String, Object> key = new HashMap<>();
            key.put("GENERATED_KEY", 7);
            kh.getKeyList().add(key);
            return 1;
        });
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of(row(7, false)));

        FiscalRangeResponse resp = svc.create(2, request(1001, 2000));

        assertThat(resp.id()).isEqualTo(7);
        verify(jdbc).execute("ALTER TABLE " + DB + ".encabezado_factura AUTO_INCREMENT = 1001");
    }

    @Test
    void update_ok_reaplicaAutoIncrement() {
        FiscalRangeService svc = service();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of(row(7, false)));
        when(jdbc.queryForObject(contains("MAX(numero_factura)"), eq(Integer.class))).thenReturn(1000);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(1);

        svc.update(2, 7, request(1500, 2500));

        verify(jdbc).execute("ALTER TABLE " + DB + ".encabezado_factura AUTO_INCREMENT = 1500");
    }

    @Test
    void delete_rangoEnUso_409() {
        FiscalRangeService svc = service();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of(row(7, true)));

        assertThatThrownBy(() -> svc.delete(2, 7))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(jdbc, never()).update(argThat((String s) -> s.startsWith("DELETE")), eq(7));
    }

    @Test
    void delete_rangoLibre_borra() {
        FiscalRangeService svc = service();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7))).thenReturn(List.of(row(7, false)));

        svc.delete(2, 7);

        verify(jdbc).update("DELETE FROM " + DB + ".datos_factura WHERE codigo_rango=?", 7);
    }

    @Test
    void get_inexistente_404() {
        FiscalRangeService svc = service();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(99))).thenReturn(List.of());

        assertThatThrownBy(() -> svc.get(2, 99))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cajaInexistente_404() {
        when(cajaCRUD.findById(55)).thenReturn(Optional.empty());
        FiscalRangeService svc = new FiscalRangeService(jdbc, cajaCRUD);

        assertThatThrownBy(() -> svc.list(55))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}

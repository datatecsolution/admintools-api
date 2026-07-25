package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.CierreResumenResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.CierreCajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CierreFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.EntradaCajaCRUD;
import net.datatecsolution.admintools.persistence.crud.SalidaCajaCRUD;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import net.datatecsolution.admintools.persistence.entity.CierreCaja;
import net.datatecsolution.admintools.persistence.entity.CierreFacturacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * US-103 — resumen del cierre con desglose por caja. El backend ya
 * consolidaba multi-caja (computeCuadre itera cajasDelUsuario); acá se
 * verifica que el desglose {@code cajas[]} expone rango + ventas por caja y
 * que los escalares de compat quedan intactos.
 *
 * Gotcha Mockito (memoria QA Hito 1): los stubs de queryForMap/queryForObject
 * con varargs van con any(Object[].class); las cajas se distinguen por
 * contains("admin_tools_caja_N") en el SQL.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CierreCajaServiceTest {

    private static final String USER = "tecnico";

    @Mock private CierreCajaCRUD cierreCRUD;
    @Mock private CierreFacturacionCRUD cierreFactCRUD;
    @Mock private SalidaCajaCRUD salidaCRUD;
    @Mock private EntradaCajaCRUD entradaCRUD;
    @Mock private CajaUsuarioCRUD cajaUsuarioCRUD;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private UsuarioCRUD usuarioCRUD;
    @Mock private DataSource commonDS;
    @Mock private PlatformTransactionManager commonTm;
    @Mock private JdbcTemplate jdbc;

    private CierreCajaService service;

    @BeforeEach
    void setUp() {
        service = new CierreCajaService(cierreCRUD, cierreFactCRUD, salidaCRUD,
                entradaCRUD, cajaUsuarioCRUD, cajaCRUD, usuarioCRUD, commonDS, commonTm);
        // el service arma su propio JdbcTemplate en el constructor; lo suplantamos
        ReflectionTestUtils.setField(service, "commonJdbc", jdbc);

        TenantContext.setTenant("admin_tools_caja_1");

        // Turno abierto id=10
        when(cierreCRUD.findFirstByUsuarioOrderByIdCierreDesc(USER))
                .thenReturn(Optional.of(cierreAbierto()));

        // Defaults inofensivos para salidas/entradas/cobros/pagos (rango vacío)
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(0);

        // Catálogo de cajas
        when(cajaCRUD.findById(1)).thenReturn(Optional.of(caja(1, "Prueba", "admin_tools_caja_1")));
        when(cajaCRUD.findById(2)).thenReturn(Optional.of(caja(2, "CAJA_SAR", "admin_tools_caja_2")));
        when(cajaCRUD.findAll()).thenReturn(List.of(
                caja(1, "Prueba", "admin_tools_caja_1"), caja(2, "CAJA_SAR", "admin_tools_caja_2")));
        when(cajaCRUD.findByNombreDb("admin_tools_caja_1"))
                .thenReturn(Optional.of(caja(1, "Prueba", "admin_tools_caja_1")));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- helpers ----------

    private static CierreCaja cierreAbierto() {
        CierreCaja c = new CierreCaja();
        c.setIdCierre(10);
        c.setEstado(1);
        c.setEfectivoInicial(new BigDecimal("500"));
        c.setTurno("A");
        c.setFechaInicio(LocalDateTime.of(2026, 7, 18, 8, 0));
        c.setNoSalidaInicial(1);
        c.setNoEntradaInicial(1);
        c.setNoCobroInicial(1);
        c.setNoPagoInicial(1);
        return c;
    }

    private static Caja caja(int codigo, String descripcion, String nombreDb) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setDescripcion(descripcion);
        c.setNombreDb(nombreDb);
        return c;
    }

    private static CierreFacturacion cf(int codigoCaja, int facturaInicial) {
        CierreFacturacion f = new CierreFacturacion();
        f.setCodigoCierre(10);
        f.setCodigoCaja(codigoCaja);
        f.setUsuario(USER);
        f.setFacturaInicial(facturaInicial);
        f.setFacturaFinal(0);
        return f;
    }

    private static Map<String, Object> ventas(String efectivo, String total, int numVentas) {
        Map<String, Object> row = new HashMap<>();
        row.put("subtotal15", new BigDecimal(total));
        row.put("subtotal18", BigDecimal.ZERO);
        row.put("cobro_tarjeta", BigDecimal.ZERO);
        row.put("cobro_efectivo", new BigDecimal(efectivo));
        row.put("subtotal_excento", BigDecimal.ZERO);
        row.put("total", new BigDecimal(total));
        row.put("impuesto", BigDecimal.ZERO);
        row.put("isv18", BigDecimal.ZERO);
        row.put("num_ventas", numVentas);
        return row;
    }

    /** Monta una caja con asignación, cf del turno, última factura y ventas. */
    private void montarCaja(int codigo, String db, int ini, int fin,
                            String efectivo, String total, int numVentas) {
        when(cierreFactCRUD.findFirstByUsuarioAndCodigoCierreAndCodigoCaja(USER, 10, codigo))
                .thenReturn(Optional.of(cf(codigo, ini)));
        when(jdbc.queryForList(argThat(s -> s != null && s.contains(db)), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(fin));
        when(jdbc.queryForMap(argThat(s -> s != null && s.contains(db)), any(Object[].class)))
                .thenReturn(ventas(efectivo, total, numVentas));
    }

    // ---------- casos ----------

    @Test
    void dosCajas_desglosePorCajaYConsolidado() {
        when(cajaUsuarioCRUD.findByIdUsuario(USER)).thenReturn(List.of(
                new CajaUsuario(1, USER, true), new CajaUsuario(2, USER, false)));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(2, 24000), cf(1, 700)));
        montarCaja(1, "admin_tools_caja_1", 700, 705, "300", "500", 6);
        montarCaja(2, "admin_tools_caja_2", 24000, 24002, "20", "20", 2);

        CierreResumenResponse r = service.resumen(USER);

        // desglose ordenado por codigo_caja
        assertThat(r.cajas()).hasSize(2);
        CierreResumenResponse.CajaResumen c1 = r.cajas().get(0);
        assertThat(c1.codigoCaja()).isEqualTo(1);
        assertThat(c1.caja()).isEqualTo("Prueba");
        assertThat(c1.noFacturaInicio()).isEqualTo(700);
        assertThat(c1.noFacturaFinal()).isEqualTo(705);
        assertThat(c1.numVentas()).isEqualTo(6);
        assertThat(c1.totalVentas()).isEqualByComparingTo("500");
        CierreResumenResponse.CajaResumen c2 = r.cajas().get(1);
        assertThat(c2.codigoCaja()).isEqualTo(2);
        assertThat(c2.noFacturaInicio()).isEqualTo(24000);
        assertThat(c2.noFacturaFinal()).isEqualTo(24002);
        assertThat(c2.numVentas()).isEqualTo(2);
        assertThat(c2.totalVentas()).isEqualByComparingTo("20");

        // consolidado = suma de ambas cajas
        assertThat(r.ventaEfectivo()).isEqualByComparingTo("320");

        // escalares de compat: nombre "2 cajas", rango de la caja del tenant
        assertThat(r.caja()).isEqualTo("2 cajas");
        assertThat(r.noFacturaInicio()).isEqualTo(700);
        assertThat(r.noFacturaFinal()).isEqualTo(705);
    }

    @Test
    void unaCaja_escalaresIdenticosAHoy() {
        when(cajaUsuarioCRUD.findByIdUsuario(USER)).thenReturn(List.of(
                new CajaUsuario(1, USER, true)));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(1, 700)));
        montarCaja(1, "admin_tools_caja_1", 700, 705, "300", "500", 6);

        CierreResumenResponse r = service.resumen(USER);

        assertThat(r.cajas()).hasSize(1);
        assertThat(r.caja()).isEqualTo("Prueba");
        assertThat(r.noFacturaInicio()).isEqualTo(700);
        assertThat(r.noFacturaFinal()).isEqualTo(705);
        assertThat(r.ventaEfectivo()).isEqualByComparingTo("300");
    }

    @Test
    void salidasDelTurno_listaEnResumen() {
        when(cajaUsuarioCRUD.findByIdUsuario(USER)).thenReturn(List.of(
                new CajaUsuario(1, USER, true)));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(1, 700)));
        montarCaja(1, "admin_tools_caja_1", 700, 705, "300", "500", 6);

        // US-108: el service consulta salidas_caja por rango+usuario+ACT con RowMapper.
        List<CierreResumenResponse.SalidaTurno> turno = List.of(
                new CierreResumenResponse.SalidaTurno(3, LocalDateTime.of(2026, 7, 18, 10, 30),
                        "Almuerzo personal", new BigDecimal("120.00")),
                new CierreResumenResponse.SalidaTurno(4, LocalDateTime.of(2026, 7, 18, 15, 0),
                        "Combustible", new BigDecimal("80.00")));
        when(jdbc.query(
                argThat((String s) -> s != null && s.contains("salidas_caja") && s.contains("estado = 'ACT'")),
                ArgumentMatchers.<RowMapper<CierreResumenResponse.SalidaTurno>>any(),
                any(Object[].class)))
                .thenReturn(turno);

        CierreResumenResponse r = service.resumen(USER);

        assertThat(r.salidas()).hasSize(2);
        assertThat(r.salidas().get(0).numero()).isEqualTo(3);
        assertThat(r.salidas().get(0).concepto()).isEqualTo("Almuerzo personal");
        assertThat(r.salidas().get(0).monto()).isEqualByComparingTo("120.00");
        assertThat(r.salidas().get(1).numero()).isEqualTo(4);
    }

    @Test
    void sinSalidasEnElTurno_listaVacia() {
        when(cajaUsuarioCRUD.findByIdUsuario(USER)).thenReturn(List.of(
                new CajaUsuario(1, USER, true)));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(1, 700)));
        montarCaja(1, "admin_tools_caja_1", 700, 705, "300", "500", 6);

        CierreResumenResponse r = service.resumen(USER);

        assertThat(r.salidas()).isEmpty();
    }

    @Test
    void cajaSinVentasEnElTurno_apareceConRangoCeroYSinVentas() {
        when(cajaUsuarioCRUD.findByIdUsuario(USER)).thenReturn(List.of(
                new CajaUsuario(1, USER, true), new CajaUsuario(2, USER, false)));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(1, 700), cf(2, 24000)));
        montarCaja(1, "admin_tools_caja_1", 700, 705, "300", "500", 6);
        // caja 2: tiene fila del turno pero SIN facturas (ultima vacía)
        when(cierreFactCRUD.findFirstByUsuarioAndCodigoCierreAndCodigoCaja(USER, 10, 2))
                .thenReturn(Optional.of(cf(2, 24000)));

        CierreResumenResponse r = service.resumen(USER);

        assertThat(r.cajas()).hasSize(2);
        CierreResumenResponse.CajaResumen c2 = r.cajas().get(1);
        assertThat(c2.noFacturaFinal()).isEqualTo(0);
        assertThat(c2.numVentas()).isEqualTo(0);
        assertThat(c2.totalVentas()).isEqualByComparingTo("0");
        // el consolidado no se contamina
        assertThat(r.ventaEfectivo()).isEqualByComparingTo("300");
    }
}

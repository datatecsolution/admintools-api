package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.CashMovementRequest;
import net.datatecsolution.admintools.domain.dto.CashMovementResponse;
import net.datatecsolution.admintools.domain.dto.CierreDetalleResponse;
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
import net.datatecsolution.admintools.persistence.entity.EntradaCaja;
import net.datatecsolution.admintools.persistence.entity.SalidaCaja;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    // ---------- US-108 p3: re-lectura para reimpresion ----------

    @Test
    void getMovimiento_salidaYEntrada_mapeanCampos() {
        SalidaCaja s = new SalidaCaja();
        s.setCodigoSalida(7);
        s.setConcepto("Almuerzo");
        s.setCantidad(new BigDecimal("120.00"));
        s.setUsuario(USER);
        s.setFecha(LocalDateTime.of(2026, 7, 18, 10, 30));
        s.setEstado("ACT");
        when(salidaCRUD.findById(7)).thenReturn(Optional.of(s));

        EntradaCaja en = new EntradaCaja();
        en.setCodigoEntrada(4);
        en.setConcepto("Cambio inicial");
        en.setCantidad(new BigDecimal("500.00"));
        en.setUsuario(USER);
        en.setFecha(LocalDateTime.of(2026, 7, 18, 8, 5));
        en.setEstado("ACT");
        when(entradaCRUD.findById(4)).thenReturn(Optional.of(en));

        CashMovementResponse salida = service.getMovimiento("salida", 7);
        assertThat(salida.id()).isEqualTo(7);
        assertThat(salida.tipo()).isEqualTo("salida");
        assertThat(salida.concepto()).isEqualTo("Almuerzo");
        assertThat(salida.monto()).isEqualByComparingTo("120.00");
        assertThat(salida.usuario()).isEqualTo(USER);

        CashMovementResponse entrada = service.getMovimiento("entrada", 4);
        assertThat(entrada.tipo()).isEqualTo("entrada");
        assertThat(entrada.monto()).isEqualByComparingTo("500.00");
    }

    @Test
    void getMovimiento_noExisteOTipoInvalido_fallan() {
        when(salidaCRUD.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMovimiento("salida", 99))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.getMovimiento("cobro", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void registrarMovimiento_entradaConCuenta_salidaConEmpleado() {
        // catálogos existen (la validación cuenta filas)
        when(jdbc.queryForObject(argThat((String s) -> s != null && s.contains("count(*)")),
                eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(entradaCRUD.save(any())).thenAnswer(inv -> {
            EntradaCaja e = inv.getArgument(0);
            e.setCodigoEntrada(21);
            return e;
        });
        when(salidaCRUD.save(any())).thenAnswer(inv -> {
            SalidaCaja s = inv.getArgument(0);
            s.setCodigoSalida(11);
            return s;
        });

        int idEntrada = service.registrarMovimiento(new CashMovementRequest(
                "entrada", new BigDecimal("500"), "Depósito", null, 4, null), USER);
        assertThat(idEntrada).isEqualTo(21);
        ArgumentCaptor<EntradaCaja> ce = ArgumentCaptor.forClass(EntradaCaja.class);
        verify(entradaCRUD).save(ce.capture());
        assertThat(ce.getValue().getCodigoCuenta()).isEqualTo(4);

        int idSalida = service.registrarMovimiento(new CashMovementRequest(
                "salida", new BigDecimal("120"), "Gasto", null, null, 3), USER);
        assertThat(idSalida).isEqualTo(11);
        ArgumentCaptor<SalidaCaja> cs = ArgumentCaptor.forClass(SalidaCaja.class);
        verify(salidaCRUD).save(cs.capture());
        assertThat(cs.getValue().getCodigoEmpleado()).isEqualTo(3);
    }

    @Test
    void registrarMovimiento_cuentaOEmpleadoInexistentes_422() {
        // el count(*) de la validación devuelve 0 (default del setUp)
        assertThatThrownBy(() -> service.registrarMovimiento(new CashMovementRequest(
                "entrada", new BigDecimal("500"), "Depósito", null, 99, null), USER))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> service.registrarMovimiento(new CashMovementRequest(
                "salida", new BigDecimal("120"), "Gasto", null, null, 99), USER))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void getMovimiento_enriqueceCuentaYEmpleado() {
        EntradaCaja en = new EntradaCaja();
        en.setCodigoEntrada(21);
        en.setCantidad(new BigDecimal("500.00"));
        en.setCodigoCuenta(4);
        when(entradaCRUD.findById(21)).thenReturn(Optional.of(en));
        when(jdbc.query(argThat((String s) -> s != null && s.contains("FROM bancos")),
                ArgumentMatchers.<RowMapper<CashMovementResponse.CuentaRef>>any(), any(Object[].class)))
                .thenReturn(List.of(new CashMovementResponse.CuentaRef(4, "Atlantida cheques", "22222", "Cheques")));

        SalidaCaja s = new SalidaCaja();
        s.setCodigoSalida(11);
        s.setCantidad(new BigDecimal("120.00"));
        s.setCodigoEmpleado(3);
        when(salidaCRUD.findById(11)).thenReturn(Optional.of(s));
        when(jdbc.query(argThat((String q) -> q != null && q.contains("FROM empleados")),
                ArgumentMatchers.<RowMapper<CashMovementResponse.EmpleadoRef>>any(), any(Object[].class)))
                .thenReturn(List.of(new CashMovementResponse.EmpleadoRef(3, "Tania Tania")));

        CashMovementResponse entrada = service.getMovimiento("entrada", 21);
        assertThat(entrada.cuenta()).isNotNull();
        assertThat(entrada.cuenta().banco()).isEqualTo("Atlantida cheques");
        assertThat(entrada.cuenta().noCuenta()).isEqualTo("22222");
        assertThat(entrada.empleado()).isNull();

        CashMovementResponse salida = service.getMovimiento("salida", 11);
        assertThat(salida.empleado()).isNotNull();
        assertThat(salida.empleado().nombre()).isEqualTo("Tania Tania");
        assertThat(salida.cuenta()).isNull();
    }

    @Test
    void getMovimiento_defaultsLegacy_sinCuentaNiEmpleado() {
        EntradaCaja en = new EntradaCaja();
        en.setCodigoEntrada(22);
        en.setCodigoCuenta(-1);
        when(entradaCRUD.findById(22)).thenReturn(Optional.of(en));

        SalidaCaja s = new SalidaCaja();
        s.setCodigoSalida(12);
        s.setCodigoEmpleado(1);
        when(salidaCRUD.findById(12)).thenReturn(Optional.of(s));

        assertThat(service.getMovimiento("entrada", 22).cuenta()).isNull();
        assertThat(service.getMovimiento("salida", 12).empleado()).isNull();
    }

    @Test
    void getCierre_numerosPersistidosYSalidasPorRangoGuardado() {
        CierreCaja c = cierreAbierto();
        c.setUsuario(USER);
        c.setEstado(0);
        c.setEfectivo(new BigDecimal("300.00"));
        c.setTarjeta(new BigDecimal("100.00"));
        c.setCreditos(new BigDecimal("50.00"));
        c.setTotalSalida(new BigDecimal("120.00"));
        c.setEfectivoCaja(new BigDecimal("650.00"));
        c.setNoSalidaInicial(3);
        c.setNoSalidaFinal(4);
        when(cierreCRUD.findById(10)).thenReturn(Optional.of(c));
        when(cierreFactCRUD.findByCodigoCierre(10)).thenReturn(List.of(cf(1, 700)));

        List<CierreResumenResponse.SalidaTurno> turno = List.of(
                new CierreResumenResponse.SalidaTurno(3, LocalDateTime.of(2026, 7, 18, 10, 30),
                        "Almuerzo", new BigDecimal("120.00")));
        when(jdbc.query(
                argThat((String s) -> s != null && s.contains("salidas_caja")),
                ArgumentMatchers.<RowMapper<CierreResumenResponse.SalidaTurno>>any(),
                any(Object[].class)))
                .thenReturn(turno);

        CierreDetalleResponse r = service.getCierre(10);

        assertThat(r.id()).isEqualTo(10);
        assertThat(r.caja()).isEqualTo("Prueba");
        assertThat(r.usuario()).isEqualTo(USER);
        assertThat(r.apertura()).isEqualByComparingTo("500");
        assertThat(r.ventaEfectivo()).isEqualByComparingTo("300.00");
        assertThat(r.totalVenta()).isEqualByComparingTo("450.00");
        assertThat(r.efectivoContado()).isEqualByComparingTo("650.00");
        assertThat(r.salidas()).hasSize(1);
        assertThat(r.salidas().get(0).numero()).isEqualTo(3);
    }

    @Test
    void getCierre_noExiste_404() {
        when(cierreCRUD.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCierre(99))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
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

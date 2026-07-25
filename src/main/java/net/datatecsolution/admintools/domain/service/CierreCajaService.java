package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.AperturaCajaRequest;
import net.datatecsolution.admintools.domain.dto.CashMovementRequest;
import net.datatecsolution.admintools.domain.dto.CashMovementResponse;
import net.datatecsolution.admintools.domain.dto.CierreActualResponse;
import net.datatecsolution.admintools.domain.dto.CierreCajaRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cierre de caja (turno) — replica de la logica del Swing:
 * {@code CtlFacturarFrame.setCierre/cierreCaja} + {@code CierreCajaDao} +
 * {@code FacturaDao.calcularCierre/getTotalCredito} +
 * {@code SalidaCajaDao/EntradaCajaDao/ReciboPagoDao/ReciboPagoProveedoresDao}.
 *
 * Modelo del turno (por usuario, multi-caja):
 *   - Apertura: cuenta el efectivo inicial e inserta cierre_caja (estado=1)
 *     con los numeros iniciales = finales del turno anterior + 1, y una fila
 *     cierre_facturacion por caja con factura_inicial = ultima final + 1.
 *   - Cierre: por cada caja del usuario suma las facturas del rango
 *     [factura_inicial, ultima_factura] en la BD de esa caja
 *     (admin_tools_caja_N.encabezado_factura) y agrega salidas/entradas/
 *     cobros/pagos de la BD comun por rango de numero + usuario; deja
 *     estado=0 y efectivo_caja = contado del arqueo.
 *
 * Las sumas por caja van con JdbcTemplate sobre el DataSource comun usando
 * nombres calificados (mismo servidor MySQL), igual que hace el Swing con
 * {@code DbName = caja.getNombreBd()}.
 */
@Service
public class CierreCajaService {

    private static final Logger log = LoggerFactory.getLogger(CierreCajaService.class);

    private final CierreCajaCRUD cierreCRUD;
    private final CierreFacturacionCRUD cierreFactCRUD;
    private final SalidaCajaCRUD salidaCRUD;
    private final EntradaCajaCRUD entradaCRUD;
    private final CajaUsuarioCRUD cajaUsuarioCRUD;
    private final CajaCRUD cajaCRUD;
    private final UsuarioCRUD usuarioCRUD;
    private final JdbcTemplate commonJdbc;
    private final TransactionTemplate commonTx;

    public CierreCajaService(CierreCajaCRUD cierreCRUD,
                             CierreFacturacionCRUD cierreFactCRUD,
                             SalidaCajaCRUD salidaCRUD,
                             EntradaCajaCRUD entradaCRUD,
                             CajaUsuarioCRUD cajaUsuarioCRUD,
                             CajaCRUD cajaCRUD,
                             UsuarioCRUD usuarioCRUD,
                             @Qualifier("commonDataSource") DataSource commonDS,
                             @Qualifier("transactionManager") PlatformTransactionManager commonTm) {
        this.cierreCRUD = cierreCRUD;
        this.cierreFactCRUD = cierreFactCRUD;
        this.salidaCRUD = salidaCRUD;
        this.entradaCRUD = entradaCRUD;
        this.cajaUsuarioCRUD = cajaUsuarioCRUD;
        this.cajaCRUD = cajaCRUD;
        this.usuarioCRUD = usuarioCRUD;
        this.commonJdbc = new JdbcTemplate(commonDS);
        this.commonTx = new TransactionTemplate(commonTm);
    }

    /* ------------------------------------------------------------------
     * Estado del turno
     * ------------------------------------------------------------------ */

    public CierreActualResponse getActual(String user) {
        return cierreCRUD.findFirstByUsuarioOrderByIdCierreDesc(user)
                .map(c -> new CierreActualResponse(c.getIdCierre(), c.getEstado() != null && c.getEstado() == 1,
                        c.getEfectivoInicial(), c.getFechaInicio(), user))
                .orElseGet(() -> new CierreActualResponse(null, false, BigDecimal.ZERO, null, user));
    }

    /* ------------------------------------------------------------------
     * Apertura (setCierre + CierreCajaDao.registrarCierre del Swing)
     * ------------------------------------------------------------------ */

    public CierreActualResponse abrir(AperturaCajaRequest req, String user) {
        return commonTx.execute(status -> {
            CierreCaja anterior = cierreCRUD.findFirstByUsuarioOrderByIdCierreDesc(user).orElse(null);
            if (anterior != null && anterior.getEstado() != null && anterior.getEstado() == 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay un turno de caja abierto.");
            }
            List<Integer> cajas = cajasDelUsuario(user);
            if (cajas.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario no tiene cajas asignadas.");
            }

            CierreCaja nuevo = new CierreCaja();
            nuevo.setFecha(LocalDate.now());
            nuevo.setFechaInicio(LocalDateTime.now());
            nuevo.setFechaFinal(LocalDateTime.now());
            nuevo.setUsuario(user);
            nuevo.setEfectivoInicial(scale2(req.efectivoInicial()));
            nuevo.setEstado(1);
            // Los contadores continuan donde quedo el turno anterior (0 si nunca hubo).
            nuevo.setNoSalidaInicial(anterior != null ? anterior.getNoSalidaFinal() + 1 : 1);
            nuevo.setNoEntradaInicial(anterior != null ? anterior.getNoEntradaFinal() + 1 : 1);
            nuevo.setNoCobroInicial(anterior != null ? anterior.getNoCobroFinal() + 1 : 1);
            nuevo.setNoPagoInicial(anterior != null ? anterior.getNoPagoFinal() + 1 : 1);
            nuevo = cierreCRUD.save(nuevo);

            for (Integer codigoCaja : cajas) {
                int facturaInicial = cierreFactCRUD
                        .findFirstByUsuarioAndCodigoCajaOrderByIdDesc(user, codigoCaja)
                        .map(prev -> prev.getFacturaFinal() + 1)
                        .orElse(1);
                CierreFacturacion cf = new CierreFacturacion();
                cf.setCodigoCierre(nuevo.getIdCierre());
                cf.setCodigoCaja(codigoCaja);
                cf.setUsuario(user);
                cf.setFacturaInicial(facturaInicial);
                cf.setFacturaFinal(0);
                cierreFactCRUD.save(cf);
            }

            log.info("CIERRE-1: turno abierto id={} usuario={} efectivoInicial={}",
                    nuevo.getIdCierre(), user, nuevo.getEfectivoInicial());
            return new CierreActualResponse(nuevo.getIdCierre(), true, nuevo.getEfectivoInicial(),
                    nuevo.getFechaInicio(), user);
        });
    }

    /* ------------------------------------------------------------------
     * Resumen (preview del cuadre, read-only)
     * ------------------------------------------------------------------ */

    public CierreResumenResponse resumen(String user) {
        CierreCaja cierre = requireAbierto(user);
        Cuadre c = computeCuadre(user, cierre);

        // US-103: desglose por caja del turno — una fila por cierre_facturacion
        // (creadas en abrir(), una por caja asignada), con su rango de folios y
        // sus ventas del cuadre. El Swing muestra lo mismo al consolidar el
        // cierre de un cajero multi-caja.
        Map<Integer, String> nombres = new HashMap<>();
        cajaCRUD.findAll().forEach(cj -> nombres.put(cj.getCodigo(), cj.getDescripcion()));
        List<CierreResumenResponse.CajaResumen> porCaja = cierreFactCRUD
                .findByCodigoCierre(cierre.getIdCierre()).stream()
                .filter(cf -> user.equals(cf.getUsuario()))
                .sorted(Comparator.comparing(CierreFacturacion::getCodigoCaja))
                .map(cf -> {
                    CajaAgg agg = c.porCaja.get(cf.getCodigoCaja());
                    return new CierreResumenResponse.CajaResumen(
                            cf.getCodigoCaja(),
                            nombres.getOrDefault(cf.getCodigoCaja(), "Caja " + cf.getCodigoCaja()),
                            cf.getFacturaInicial(),
                            agg != null ? agg.facturaFinal : 0,
                            agg != null ? agg.numVentas : 0,
                            agg != null ? agg.totalVentas : BigDecimal.ZERO);
                })
                .collect(Collectors.toList());

        // Escalares de compat: con una sola caja, identicos a siempre; con
        // varias, el nombre pasa a "N cajas" y el rango sigue siendo el de la
        // caja de la sesion (tenant del JWT) para no romper el POS viejo.
        String cajaNombre = porCaja.size() == 1 ? porCaja.get(0).caja()
                : porCaja.size() > 1 ? porCaja.size() + " cajas" : "—";
        Integer noIni = 0;
        Integer noFin = 0;
        String tenant = TenantContext.getTenant();
        if (tenant != null && !tenant.isBlank()) {
            Caja caja = cajaCRUD.findByNombreDb(tenant).orElse(null);
            if (caja != null) {
                if (porCaja.isEmpty()) cajaNombre = caja.getDescripcion();
                CierreFacturacion cf = cierreFactCRUD
                        .findFirstByUsuarioAndCodigoCierreAndCodigoCaja(user, cierre.getIdCierre(), caja.getCodigo())
                        .orElse(null);
                if (cf != null) {
                    noIni = cf.getFacturaInicial();
                    CajaAgg agg = c.porCaja.get(caja.getCodigo());
                    noFin = agg != null ? agg.facturaFinal : 0;
                }
            }
        }

        return new CierreResumenResponse(
                cajaNombre, user, cierre.getTurno(),
                String.valueOf(cierre.getFechaInicio()),
                cierre.getEfectivoInicial(),
                c.efectivo, c.tarjeta, c.credito,
                c.exento, c.isv15, c.isv18,
                c.totalCobro, c.totalEntrada, c.totalSalida, c.totalPago,
                noIni, noFin,
                porCaja,
                salidasDelTurno(user, cierre.getNoSalidaInicial(), c.noSalidaFinal));
    }

    /**
     * US-108: salidas de caja del turno (subreporte cierre_salida del Swing) —
     * mismo rango [no_salida_inicial, ultima del usuario] y filtro estado ACT
     * que el escalar totalSalida, para que la lista cuadre con el total.
     */
    private List<CierreResumenResponse.SalidaTurno> salidasDelTurno(String user, int ini, int fin) {
        return commonJdbc.query(
                "SELECT codigo_salida, fecha, concepto, cantidad FROM salidas_caja "
                        + "WHERE codigo_salida >= ? AND codigo_salida <= ? AND usuario = ? AND estado = 'ACT' "
                        + "ORDER BY codigo_salida",
                (rs, i) -> new CierreResumenResponse.SalidaTurno(
                        rs.getInt("codigo_salida"),
                        rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                        rs.getString("concepto"),
                        rs.getBigDecimal("cantidad")),
                ini, fin, user);
    }

    /* ------------------------------------------------------------------
     * Cierre (verificarCierrePendiente + actualizarCierre del Swing)
     * ------------------------------------------------------------------ */

    public CierreActualResponse cerrar(CierreCajaRequest req, String user) {
        return commonTx.execute(status -> {
            CierreCaja cierre = requireAbierto(user);

            // verificarCierrePendiente: alguna caja con facturas desde el inicio
            // del rango; si una caja no tiene fila cierre_facturacion pero si
            // facturas, se crea la fila (reparacion legacy, igual que el Swing).
            boolean pendiente = false;
            for (Integer codigoCaja : cajasDelUsuario(user)) {
                Caja caja = cajaCRUD.findById(codigoCaja).orElse(null);
                if (caja == null) continue;
                String db = safeDb(caja.getNombreDb());
                CierreFacturacion cf = cierreFactCRUD
                        .findFirstByUsuarioAndCodigoCierreAndCodigoCaja(user, cierre.getIdCierre(), codigoCaja)
                        .orElse(null);
                if (cf != null) {
                    Integer n = commonJdbc.queryForObject(
                            "SELECT count(*) FROM " + db + ".encabezado_factura WHERE numero_factura >= ? AND usuario = ?",
                            Integer.class, cf.getFacturaInicial(), user);
                    if (n != null && n > 0) pendiente = true;
                } else {
                    Integer n = commonJdbc.queryForObject(
                            "SELECT count(*) FROM " + db + ".encabezado_factura WHERE usuario = ?",
                            Integer.class, user);
                    if (n != null && n > 0) {
                        CierreFacturacion repara = new CierreFacturacion();
                        repara.setCodigoCierre(cierre.getIdCierre());
                        repara.setCodigoCaja(codigoCaja);
                        repara.setUsuario(user);
                        repara.setFacturaInicial(0);
                        repara.setFacturaFinal(0);
                        cierreFactCRUD.save(repara);
                    }
                }
            }
            if (!pendiente) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay facturas para crear un cierre de caja.");
            }

            Cuadre c = computeCuadre(user, cierre);

            cierre.setEfectivo(scale2(c.efectivo));
            cierre.setCreditos(scale2(c.credito));
            cierre.setTarjeta(scale2(c.tarjeta));
            cierre.setIsv15(scale2(c.isv15));
            cierre.setIsv18(scale2(c.isv18));
            cierre.setTotalIsv15(scale2(c.base15));
            cierre.setTotalIsv18(scale2(c.base18));
            cierre.setTotalExcento(scale2(c.exento));
            cierre.setTotalVenta(scale2(c.total));
            cierre.setNoSalidaFinal(c.noSalidaFinal);
            cierre.setTotalSalida(scale2(c.totalSalida));
            cierre.setNoEntradaFinal(c.noEntradaFinal);
            cierre.setTotalEntrada(scale2(c.totalEntrada));
            cierre.setNoCobroFinal(c.noCobroFinal);
            cierre.setTotalCobro(scale2(c.totalCobro));
            cierre.setNoPagoFinal(c.noPagoFinal);
            cierre.setTotalPago(scale2(c.totalPago));
            // total_efectivo: inicial + ventas efectivo - salidas + entradas + cobros - pagos
            BigDecimal totalEfectivo = cierre.getEfectivoInicial()
                    .add(c.efectivo)
                    .subtract(c.totalSalida)
                    .add(c.totalEntrada)
                    .add(c.totalCobro)
                    .subtract(c.totalPago);
            cierre.setTotalEfectivo(scale2(totalEfectivo));
            cierre.setEfectivoCaja(scale2(req.contado()));
            cierre.setEstado(0);
            cierre.setFechaFinal(LocalDateTime.now());
            cierreCRUD.save(cierre);

            // Completa el rango por caja con la ultima factura del turno.
            for (Map.Entry<Integer, CajaAgg> e : c.porCaja.entrySet()) {
                cierreFactCRUD.findFirstByUsuarioAndCodigoCierreAndCodigoCaja(user, cierre.getIdCierre(), e.getKey())
                        .ifPresent(cf -> {
                            cf.setFacturaFinal(e.getValue().facturaFinal);
                            cierreFactCRUD.save(cf);
                        });
            }

            log.info("CIERRE-2: turno cerrado id={} usuario={} contado={} esperado={}",
                    cierre.getIdCierre(), user, cierre.getEfectivoCaja(), cierre.getTotalEfectivo());
            return new CierreActualResponse(cierre.getIdCierre(), false, cierre.getEfectivoInicial(),
                    cierre.getFechaInicio(), user);
        });
    }

    /* ------------------------------------------------------------------
     * Movimientos de caja (SalidaCajaDao/EntradaCajaDao.registrar)
     * ------------------------------------------------------------------ */

    public int registrarMovimiento(CashMovementRequest req, String user) {
        requireAbierto(user);
        String concepto = req.concepto() != null && !req.concepto().isBlank() ? req.concepto()
                : (req.motivo() != null && !req.motivo().isBlank() ? req.motivo() : "NA");
        if ("salida".equals(req.tipo())) {
            SalidaCaja s = new SalidaCaja();
            s.setConcepto(concepto);
            s.setCantidad(scale2(req.monto()));
            s.setUsuario(user);
            s.setFecha(LocalDateTime.now());
            // US-108: empleado elegido en el POS; sin elegir cae al default
            // legacy (empleado del usuario, o 1 = "system" = sin empleado).
            s.setCodigoEmpleado(req.empleadoId() != null
                    ? requireExists("empleados", "codigo_empleado", req.empleadoId(), "Empleado inválido.")
                    : codigoEmpleado(user));
            s.setEstado("ACT");
            s.setCodigoCuenta(-1);
            return salidaCRUD.save(s).getCodigoSalida();
        }
        EntradaCaja e = new EntradaCaja();
        e.setConcepto(concepto);
        e.setCantidad(scale2(req.monto()));
        e.setUsuario(user);
        e.setFecha(LocalDateTime.now());
        e.setEstado("ACT");
        // US-108: cuenta bancaria destino de la entrada (opcional; -1 = sin cuenta).
        e.setCodigoCuenta(req.cuentaId() != null
                ? requireExists("bancos", "id", req.cuentaId(), "Cuenta bancaria inválida.")
                : -1);
        return entradaCRUD.save(e).getCodigoEntrada();
    }

    /** 422 si el id no existe en el catálogo; devuelve el id validado. */
    private int requireExists(String table, String idCol, int id, String message) {
        Integer n = commonJdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + idCol + " = ?", Integer.class, id);
        if (n == null || n == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return id;
    }

    /* ------------------------------------------------------------------
     * US-108: re-lectura para reimpresion de comprobantes
     * ------------------------------------------------------------------ */

    /** Movimiento por id. El tipo es obligatorio: entradas y salidas numeran aparte. */
    public CashMovementResponse getMovimiento(String tipo, int id) {
        if ("salida".equals(tipo)) {
            return salidaCRUD.findById(id)
                    .map(s -> new CashMovementResponse(s.getCodigoSalida(), "salida", s.getFecha(),
                            s.getConcepto(), s.getCantidad(), s.getUsuario(), s.getEstado(),
                            null, empleadoRef(s.getCodigoEmpleado())))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Salida de caja no encontrada."));
        }
        if ("entrada".equals(tipo)) {
            return entradaCRUD.findById(id)
                    .map(e -> new CashMovementResponse(e.getCodigoEntrada(), "entrada", e.getFecha(),
                            e.getConcepto(), e.getCantidad(), e.getUsuario(), e.getEstado(),
                            cuentaRef(e.getCodigoCuenta()), null))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Entrada de caja no encontrada."));
        }
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "tipo debe ser 'entrada' o 'salida'.");
    }

    /** Cuenta destino de una entrada; null con el default legacy (-1). */
    private CashMovementResponse.CuentaRef cuentaRef(Integer codigoCuenta) {
        if (codigoCuenta == null || codigoCuenta <= 0) return null;
        List<CashMovementResponse.CuentaRef> rows = commonJdbc.query(
                "SELECT b.id, b.nombre, b.no_cuenta, tc.tipo_cuenta FROM bancos b "
                        + "LEFT JOIN tipo_cuenta_bancos tc ON b.id_tipo_cuenta = tc.id "
                        + "WHERE b.id = ?",
                (rs, i) -> new CashMovementResponse.CuentaRef(
                        rs.getInt("id"), rs.getString("nombre"),
                        rs.getString("no_cuenta") != null && !"NA".equalsIgnoreCase(rs.getString("no_cuenta"))
                                ? rs.getString("no_cuenta") : null,
                        rs.getString("tipo_cuenta")),
                codigoCuenta);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Empleado de una salida; null con el default legacy (1 = "system"). */
    private CashMovementResponse.EmpleadoRef empleadoRef(Integer codigoEmpleado) {
        if (codigoEmpleado == null || codigoEmpleado <= 1) return null;
        List<CashMovementResponse.EmpleadoRef> rows = commonJdbc.query(
                "SELECT codigo_empleado, trim(concat(nombre, ' ', apellido)) AS nombre "
                        + "FROM empleados WHERE codigo_empleado = ?",
                (rs, i) -> new CashMovementResponse.EmpleadoRef(
                        rs.getInt("codigo_empleado"), rs.getString("nombre")),
                codigoEmpleado);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Cierre por id, con los numeros persistidos al cerrar (sin recalcular) y
     * las salidas del turno por el rango guardado. Con el turno aun abierto
     * los totales van en 0 y las salidas vacias (para eso esta el resumen).
     */
    public CierreDetalleResponse getCierre(int id) {
        CierreCaja c = cierreCRUD.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cierre de caja no encontrado."));

        // Nombre de caja con la convencion del resumen: 1 → nombre, N → "N cajas".
        Map<Integer, String> nombres = new HashMap<>();
        cajaCRUD.findAll().forEach(cj -> nombres.put(cj.getCodigo(), cj.getDescripcion()));
        List<Integer> cajas = cierreFactCRUD.findByCodigoCierre(c.getIdCierre()).stream()
                .map(CierreFacturacion::getCodigoCaja)
                .distinct().sorted().toList();
        String caja = cajas.size() == 1 ? nombres.getOrDefault(cajas.get(0), "Caja " + cajas.get(0))
                : cajas.size() > 1 ? cajas.size() + " cajas" : "—";

        BigDecimal totalVenta = nz(c.getEfectivo()).add(nz(c.getTarjeta())).add(nz(c.getCreditos()));
        return new CierreDetalleResponse(
                c.getIdCierre(), caja, c.getUsuario(), c.getTurno(), c.getEstado(),
                c.getFechaInicio(), c.getFechaFinal(),
                c.getEfectivoInicial(), c.getEfectivo(),
                c.getTotalCobro(), c.getTotalEntrada(), c.getTotalSalida(), c.getTotalPago(),
                c.getTotalEfectivo(), c.getEfectivoCaja(),
                c.getTotalExcento(), c.getIsv15(), c.getIsv18(),
                c.getTarjeta(), c.getCreditos(), totalVenta,
                salidasDelTurno(c.getUsuario(),
                        c.getNoSalidaInicial() != null ? c.getNoSalidaInicial() : 0,
                        c.getNoSalidaFinal() != null ? c.getNoSalidaFinal() : 0));
    }

    /* ------------------------------------------------------------------
     * Calculo del cuadre (FacturaDao.calcularCierre + getTotalCredito +
     * totales de salidas/entradas/cobros/pagos por rango)
     * ------------------------------------------------------------------ */

    private static class Cuadre {
        BigDecimal efectivo = BigDecimal.ZERO;   // SUM(cobro_efectivo)
        BigDecimal tarjeta = BigDecimal.ZERO;    // SUM(cobro_tarjeta)
        BigDecimal credito = BigDecimal.ZERO;    // SUM(total) tipo_factura=2
        BigDecimal exento = BigDecimal.ZERO;     // SUM(subtotal_excento)
        BigDecimal base15 = BigDecimal.ZERO;     // SUM(subtotal15)
        BigDecimal base18 = BigDecimal.ZERO;     // SUM(subtotal18)
        BigDecimal isv15 = BigDecimal.ZERO;      // SUM(impuesto)
        BigDecimal isv18 = BigDecimal.ZERO;      // SUM(isv18)
        BigDecimal total = BigDecimal.ZERO;      // SUM(total)
        // US-103: agregado por caja (rango + ventas) para el desglose del resumen.
        Map<Integer, CajaAgg> porCaja = new HashMap<>();
        int noSalidaFinal;
        BigDecimal totalSalida = BigDecimal.ZERO;
        int noEntradaFinal;
        BigDecimal totalEntrada = BigDecimal.ZERO;
        int noCobroFinal;
        BigDecimal totalCobro = BigDecimal.ZERO;
        int noPagoFinal;
        BigDecimal totalPago = BigDecimal.ZERO;
    }

    /** US-103: acumulado de una caja del turno (para el desglose por caja). */
    private static class CajaAgg {
        int facturaFinal;
        int numVentas;
        BigDecimal totalVentas = BigDecimal.ZERO;
    }

    private Cuadre computeCuadre(String user, CierreCaja cierre) {
        Cuadre c = new Cuadre();

        for (Integer codigoCaja : cajasDelUsuario(user)) {
            Caja caja = cajaCRUD.findById(codigoCaja).orElse(null);
            if (caja == null) continue;
            String db = safeDb(caja.getNombreDb());

            List<Integer> ultima = commonJdbc.queryForList(
                    "SELECT numero_factura FROM " + db + ".encabezado_factura "
                            + "WHERE usuario = ? ORDER BY numero_factura DESC LIMIT 1",
                    Integer.class, user);
            CierreFacturacion cf = cierreFactCRUD
                    .findFirstByUsuarioAndCodigoCierreAndCodigoCaja(user, cierre.getIdCierre(), codigoCaja)
                    .orElse(null);
            if (ultima.isEmpty() || cf == null) continue;

            int ini = cf.getFacturaInicial();
            int fin = ultima.get(0);
            CajaAgg agg = new CajaAgg();
            agg.facturaFinal = fin;
            c.porCaja.put(codigoCaja, agg);

            Map<String, Object> row = commonJdbc.queryForMap(
                    "SELECT ifnull(sum(subtotal15),0) AS subtotal15, ifnull(sum(subtotal18),0) AS subtotal18, "
                            + "ifnull(sum(cobro_tarjeta),0) AS cobro_tarjeta, ifnull(sum(cobro_efectivo),0) AS cobro_efectivo, "
                            + "ifnull(sum(subtotal_excento),0) AS subtotal_excento, ifnull(sum(total),0) AS total, "
                            + "ifnull(sum(impuesto),0) AS impuesto, ifnull(sum(isv18),0) AS isv18, "
                            + "count(*) AS num_ventas "
                            + "FROM " + db + ".encabezado_factura "
                            + "WHERE usuario = ? AND numero_factura >= ? AND numero_factura <= ? AND estado_factura = 'ACT'",
                    user, ini, fin);
            agg.numVentas = ((Number) row.getOrDefault("num_ventas", 0)).intValue();
            agg.totalVentas = bd(row, "total");
            c.efectivo = c.efectivo.add(bd(row, "cobro_efectivo"));
            c.tarjeta = c.tarjeta.add(bd(row, "cobro_tarjeta"));
            c.exento = c.exento.add(bd(row, "subtotal_excento"));
            c.base15 = c.base15.add(bd(row, "subtotal15"));
            c.base18 = c.base18.add(bd(row, "subtotal18"));
            c.isv15 = c.isv15.add(bd(row, "impuesto"));
            c.isv18 = c.isv18.add(bd(row, "isv18"));
            c.total = c.total.add(bd(row, "total"));

            BigDecimal credito = commonJdbc.queryForObject(
                    "SELECT ifnull(sum(total),0) FROM " + db + ".encabezado_factura "
                            + "WHERE tipo_factura = 2 AND estado_factura = 'ACT' "
                            + "AND numero_factura >= ? AND numero_factura <= ? AND usuario = ?",
                    BigDecimal.class, ini, fin, user);
            c.credito = c.credito.add(nz(credito));
        }

        // Salidas / entradas: ultimo numero del usuario + suma del rango (estado ACT).
        c.noSalidaFinal = lastNumber("salidas_caja", "codigo_salida", user);
        c.totalSalida = nz(commonJdbc.queryForObject(
                "SELECT ifnull(sum(cantidad),0) FROM salidas_caja "
                        + "WHERE codigo_salida >= ? AND codigo_salida <= ? AND usuario = ? AND estado = 'ACT'",
                BigDecimal.class, cierre.getNoSalidaInicial(), c.noSalidaFinal, user));

        c.noEntradaFinal = lastNumber("entradas_caja", "codigo_entrada", user);
        c.totalEntrada = nz(commonJdbc.queryForObject(
                "SELECT ifnull(sum(cantidad),0) FROM entradas_caja "
                        + "WHERE codigo_entrada >= ? AND codigo_entrada <= ? AND usuario = ? AND estado = 'ACT'",
                BigDecimal.class, cierre.getNoEntradaInicial(), c.noEntradaFinal, user));

        // Cobros a clientes / pagos a proveedores (sin filtro de estado, como el Swing).
        c.noCobroFinal = lastNumber("recibo_pago", "no_recibo", user);
        c.totalCobro = nz(commonJdbc.queryForObject(
                "SELECT ifnull(sum(total),0) FROM recibo_pago "
                        + "WHERE no_recibo >= ? AND no_recibo <= ? AND usuario = ?",
                BigDecimal.class, cierre.getNoCobroInicial(), c.noCobroFinal, user));

        c.noPagoFinal = lastNumber("recibo_pago_proveedores", "no_recibo", user);
        c.totalPago = nz(commonJdbc.queryForObject(
                "SELECT ifnull(sum(total),0) FROM recibo_pago_proveedores "
                        + "WHERE no_recibo >= ? AND no_recibo <= ? AND usuario = ?",
                BigDecimal.class, cierre.getNoPagoInicial(), c.noPagoFinal, user));

        return c;
    }

    /* ------------------------------------------------------------------ */

    /**
     * Cajas habilitadas del usuario: cajas_usuarios (Sprint 4.5); si el
     * usuario aun no esta migrado al modelo multi-caja, cae a
     * usuario.codigo_caja (modelo viejo, una sola caja).
     */
    private List<Integer> cajasDelUsuario(String user) {
        List<Integer> cajas = cajaUsuarioCRUD.findByIdUsuario(user).stream()
                .map(CajaUsuario::getCodigoCaja)
                .toList();
        if (!cajas.isEmpty()) return cajas;
        return usuarioCRUD.findByNombreUsuario(user)
                .map(u -> u.getCodigoCaja() != null && u.getCodigoCaja() > 0
                        ? List.of(u.getCodigoCaja())
                        : List.<Integer>of())
                .orElseGet(List::of);
    }

    private CierreCaja requireAbierto(String user) {
        CierreCaja cierre = cierreCRUD.findFirstByUsuarioOrderByIdCierreDesc(user).orElse(null);
        if (cierre == null || cierre.getEstado() == null || cierre.getEstado() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No tiene un turno de caja abierto.");
        }
        return cierre;
    }

    private int lastNumber(String table, String idCol, String user) {
        List<Integer> last = commonJdbc.queryForList(
                "SELECT " + idCol + " FROM " + table + " WHERE usuario = ? ORDER BY " + idCol + " DESC LIMIT 1",
                Integer.class, user);
        return last.isEmpty() ? 0 : last.get(0);
    }

    private Integer codigoEmpleado(String user) {
        return usuarioCRUD.findByNombreUsuario(user)
                .map(u -> u.getCodigoEmpleado() != null ? u.getCodigoEmpleado() : 1)
                .orElse(1);
    }

    /** El nombre de BD viene de la tabla cajas; se valida igual por defensa. */
    private static String safeDb(String nombreDb) {
        if (nombreDb == null || !nombreDb.matches("[A-Za-z0-9_]+")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Caja con nombre de BD invalido.");
        }
        return "`" + nombreDb + "`";
    }

    private static BigDecimal bd(Map<String, Object> row, String col) {
        Object v = row.get(col);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal scale2(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_EVEN);
    }
}

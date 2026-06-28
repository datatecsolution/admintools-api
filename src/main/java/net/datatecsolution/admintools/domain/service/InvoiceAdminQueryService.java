package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.InvoiceAdminDetailResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceAdminSummaryResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceListItem;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sección Facturas (US-041) — lista las facturas de una caja ELEGIDA, sin
 * depender del tenant del usuario, porque admin/supervisor no tienen caja y
 * ven todas (igual que CtlFacturas del Swing: combo de cajas). Consulta
 * cross-DB la {@code <caja>.encabezado_factura} con el nombre de cliente de
 * {@code admin_tools.cliente}, más reciente primero, con filtros por estado,
 * rango de fechas y nombre/código de cliente.
 */
@Service
public class InvoiceAdminQueryService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public InvoiceAdminQueryService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.cajaCRUD = cajaCRUD;
    }

    /** Nombre de BD de una caja, validado contra inyección (uso en SQL cross-DB). */
    private String resolveDb(int cajaCode) {
        String db = cajaCRUD.findById(cajaCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caja no encontrada"))
                .getNombreDb();
        if (db == null || !SAFE_DB.matcher(db).matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nombre de BD de caja inválido");
        }
        return db;
    }

    /** Filtro común (estado/fechas/búsqueda) + sus valores por caja. */
    private record Filter(String where, List<Object> values) {}

    /** Construye el WHERE y los valores. Búsqueda cubre nombre/código/Nº/RTN. */
    private Filter buildFilter(String estado, LocalDate from, LocalDate to, String search) {
        StringBuilder w = new StringBuilder(" WHERE 1=1 ");
        List<Object> v = new ArrayList<>();
        if (estado != null && !"ALL".equalsIgnoreCase(estado)) { w.append(" AND e.estado_factura = ? "); v.add(estado); }
        if (from != null) { w.append(" AND e.fecha >= ? "); v.add(from.atStartOfDay()); }
        if (to != null)   { w.append(" AND e.fecha < ? ");  v.add(to.plusDays(1).atStartOfDay()); }
        if (search != null && !search.isBlank()) {
            w.append(" AND (cli.nombre_cliente LIKE ? OR e.codigo_cliente LIKE ? "
                    + "OR CAST(e.numero_factura AS CHAR) LIKE ? OR cli.rtn LIKE ?) ");
            String like = "%" + search.trim() + "%";
            v.add(like); v.add(like); v.add(like); v.add(like);
        }
        return new Filter(w.toString(), v);
    }

    /** SELECT de una caja (con la columna `caja` literal) para la UNION. */
    private String cajaSelect(int cajaCode, String where) {
        String db = resolveDb(cajaCode);
        return " SELECT e.numero_factura, e.fecha, e.codigo_cliente, cli.nombre_cliente, cli.rtn, "
                + " TRIM(CONCAT_WS(' ', emp.nombre, emp.apellido)) AS nombre_vendedor, "
                + " e.total, e.estado_factura, e.tipo_factura, " + cajaCode + " AS caja "
                + " FROM " + db + ".encabezado_factura e "
                + " LEFT JOIN admin_tools.cliente cli ON cli.codigo_cliente = e.codigo_cliente "
                + " LEFT JOIN admin_tools.empleados emp ON emp.codigo_empleado = e.codigo_vendedor "
                + where;
    }

    /** caja != null → esa caja; null → TODAS las cajas registradas (consolidado). */
    private List<Integer> resolveCajas(Integer caja) {
        if (caja != null) return List.of(caja);
        List<Integer> ids = new ArrayList<>();
        for (Caja c : cajaCRUD.findAll()) {
            if (c.getCodigo() != null && c.getNombreDb() != null) ids.add(c.getCodigo());
        }
        return ids;
    }

    private record Union(String sql, Object[] args) {}

    private Union buildUnion(List<Integer> cajas, Filter f) {
        List<String> parts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (int c : cajas) {
            parts.add("(" + cajaSelect(c, f.where()) + ")");
            args.addAll(f.values());
        }
        return new Union(String.join(" UNION ALL ", parts), args.toArray());
    }

    /**
     * Lista consolidada y paginada SERVER-SIDE sobre todas las cajas (o una).
     * estado: ACT | NULA | ALL (default ACT). Búsqueda por nombre/código/Nº/RTN.
     */
    public Page<InvoiceListItem> list(Integer caja, String search, LocalDate from, LocalDate to,
                                      String estado, Pageable pageable) {
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        Union u = buildUnion(cajas, buildFilter(estado, from, to, search));

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + u.sql() + ") u", Long.class, u.args());
        long totalElements = total != null ? total : 0L;

        List<Object> pageArgs = new ArrayList<>(List.of(u.args()));
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<InvoiceListItem> rows = jdbc.query(
                "SELECT * FROM (" + u.sql() + ") u ORDER BY u.fecha DESC, u.numero_factura DESC LIMIT ? OFFSET ?",
                (rs, i) -> new InvoiceListItem(
                        rs.getInt("numero_factura"),
                        rs.getInt("caja"),
                        rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                        rs.getString("codigo_cliente"),
                        rs.getString("nombre_cliente"),
                        rs.getString("rtn"),
                        rs.getString("nombre_vendedor"),
                        rs.getBigDecimal("total"),
                        rs.getString("estado_factura"),
                        (Integer) rs.getObject("tipo_factura")),
                pageArgs.toArray());

        return new PageImpl<>(rows, pageable, totalElements);
    }

    /** Total server-side (count + suma, excluyendo NULA) del filtro, sobre todas las cajas. */
    public InvoiceAdminSummaryResponse summary(Integer caja, String search, LocalDate from, LocalDate to,
                                               String estado) {
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) return new InvoiceAdminSummaryResponse(0L, BigDecimal.ZERO);
        Union u = buildUnion(cajas, buildFilter(estado, from, to, search));
        return jdbc.queryForObject(
                "SELECT COUNT(*) AS cnt, "
                        + "COALESCE(SUM(CASE WHEN u.estado_factura <> 'NULA' THEN u.total ELSE 0 END), 0) AS total "
                        + "FROM (" + u.sql() + ") u",
                (rs, i) -> new InvoiceAdminSummaryResponse(rs.getLong("cnt"), rs.getBigDecimal("total")),
                u.args());
    }

    /**
     * Detalle completo de una factura de una caja elegida (drawer del rediseño,
     * US-041). Header con cliente/RTN/vendedor/cajero + totales, líneas
     * agregadas por artículo con su nombre y lo ya devuelto (kardex). 404 si la
     * factura no existe en esa caja.
     */
    public InvoiceAdminDetailResponse detail(int cajaCode, int numero) {
        String db = resolveDb(cajaCode);

        InvoiceAdminDetailResponse header;
        try {
            header = jdbc.queryForObject(
                    "SELECT e.numero_factura, e.fecha, e.estado_factura, e.tipo_factura, e.tipo_pago, "
                            + "e.codigo_cliente, c.nombre_cliente, c.rtn, "
                            + "TRIM(CONCAT_WS(' ', emp.nombre, emp.apellido)) AS nombre_vendedor, "
                            + "e.usuario, e.subtotal, e.impuesto, e.descuento, e.total, e.pago, e.observacion "
                            + "FROM " + db + ".encabezado_factura e "
                            + "LEFT JOIN admin_tools.cliente c ON c.codigo_cliente = e.codigo_cliente "
                            + "LEFT JOIN admin_tools.empleados emp ON emp.codigo_empleado = e.codigo_vendedor "
                            + "WHERE e.numero_factura = ?",
                    (rs, i) -> {
                        BigDecimal total = nz(rs.getBigDecimal("total"));
                        BigDecimal pago = nz(rs.getBigDecimal("pago"));
                        BigDecimal cambio = pago.subtract(total).max(BigDecimal.ZERO);
                        return new InvoiceAdminDetailResponse(
                                rs.getInt("numero_factura"),
                                cajaCode,
                                rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                                rs.getString("estado_factura"),
                                (Integer) rs.getObject("tipo_factura"),
                                (Integer) rs.getObject("tipo_pago"),
                                rs.getString("codigo_cliente"),
                                rs.getString("nombre_cliente"),
                                rs.getString("rtn"),
                                rs.getString("nombre_vendedor"),
                                rs.getString("usuario"),
                                nz(rs.getBigDecimal("subtotal")),
                                nz(rs.getBigDecimal("impuesto")),
                                nz(rs.getBigDecimal("descuento")),
                                total, pago, cambio,
                                rs.getString("observacion"),
                                List.of());
                    },
                    numero);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Factura " + numero + " no existe en la caja " + cajaCode);
        }

        // Cantidades ya devueltas por artículo (kardex), para el chip "−N devuelto".
        Map<Integer, BigDecimal> devueltas = new LinkedHashMap<>();
        jdbc.query(
                "SELECT codigo_articulo, SUM(cantidad) AS dev FROM admin_tools.detalle_devoluciones "
                        + "WHERE numero_factura = ? AND codigo_caja = ? GROUP BY codigo_articulo",
                rs -> { devueltas.put(rs.getInt("codigo_articulo"), nz(rs.getBigDecimal("dev"))); },
                numero, cajaCode);

        List<InvoiceAdminDetailResponse.Linea> lineas = jdbc.query(
                "SELECT d.codigo_articulo, a.articulo, SUM(d.cantidad) AS cantidad, d.precio, SUM(d.total) AS total "
                        + "FROM " + db + ".detalle_factura d "
                        + "LEFT JOIN admin_tools.articulo a ON a.codigo_articulo = d.codigo_articulo "
                        + "WHERE d.numero_factura = ? "
                        + "GROUP BY d.codigo_articulo, a.articulo, d.precio "
                        + "ORDER BY d.codigo_articulo",
                (rs, i) -> {
                    int art = rs.getInt("codigo_articulo");
                    String nombre = rs.getString("articulo");
                    return new InvoiceAdminDetailResponse.Linea(
                            art,
                            nombre != null ? nombre : "Art. " + art,
                            nz(rs.getBigDecimal("cantidad")),
                            nz(rs.getBigDecimal("precio")),
                            nz(rs.getBigDecimal("total")),
                            devueltas.getOrDefault(art, BigDecimal.ZERO));
                },
                numero);

        return new InvoiceAdminDetailResponse(
                header.numeroFactura(), header.codigoCaja(), header.fecha(), header.estadoFactura(),
                header.tipoFactura(), header.tipoPago(), header.codigoCliente(), header.nombreCliente(),
                header.rtn(), header.nombreVendedor(), header.usuario(), header.subtotal(), header.impuesto(),
                header.descuento(), header.total(), header.pago(), header.cambio(), header.observacion(),
                lineas);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}

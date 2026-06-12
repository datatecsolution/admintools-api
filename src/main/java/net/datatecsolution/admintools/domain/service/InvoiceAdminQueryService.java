package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.InvoiceListItem;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    /** estado: ACT | NULA | ALL (default ACT). */
    public Page<InvoiceListItem> list(int cajaCode, String search, LocalDate from, LocalDate to,
                                      String estado, Pageable pageable) {
        var caja = cajaCRUD.findById(cajaCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caja no encontrada"));
        String db = caja.getNombreDb();
        if (db == null || !SAFE_DB.matcher(db).matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nombre de BD de caja inválido");
        }

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (estado != null && !"ALL".equalsIgnoreCase(estado)) {
            where.append(" AND e.estado_factura = ? ");
            args.add(estado);
        }
        if (from != null) { where.append(" AND e.fecha >= ? "); args.add(from.atStartOfDay()); }
        if (to != null)   { where.append(" AND e.fecha < ? ");  args.add(to.plusDays(1).atStartOfDay()); }
        if (search != null && !search.isBlank()) {
            where.append(" AND (c.nombre_cliente LIKE ? OR e.codigo_cliente LIKE ?) ");
            String like = "%" + search.trim() + "%";
            args.add(like); args.add(like);
        }

        String base = " FROM " + db + ".encabezado_factura e "
                + " LEFT JOIN admin_tools.cliente c ON c.codigo_cliente = e.codigo_cliente "
                + where;

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + base, Long.class, args.toArray());
        long totalElements = total != null ? total : 0L;

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageable.getPageSize());
        pageArgs.add(pageable.getOffset());
        List<InvoiceListItem> rows = jdbc.query(
                "SELECT e.numero_factura, e.fecha, e.codigo_cliente, c.nombre_cliente, "
                        + "e.total, e.estado_factura, e.tipo_factura " + base
                        + " ORDER BY e.numero_factura DESC LIMIT ? OFFSET ?",
                (rs, i) -> new InvoiceListItem(
                        rs.getInt("numero_factura"),
                        cajaCode,
                        rs.getTimestamp("fecha") != null ? rs.getTimestamp("fecha").toLocalDateTime() : null,
                        rs.getString("codigo_cliente"),
                        rs.getString("nombre_cliente"),
                        rs.getBigDecimal("total"),
                        rs.getString("estado_factura"),
                        (Integer) rs.getObject("tipo_factura")),
                pageArgs.toArray());

        return new PageImpl<>(rows, pageable, totalElements);
    }
}

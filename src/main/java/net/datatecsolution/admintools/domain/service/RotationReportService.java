package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.RotationClassSummary;
import net.datatecsolution.admintools.domain.dto.RotationItem;
import net.datatecsolution.admintools.domain.dto.RotationReportResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-063 — rotación de inventario por producto: unidades vendidas del período
 * (todas las cajas o una) contra el stock actual (saldo materializado de
 * {@code existencia_articulo_bodega}, US-131/132), con clasificación
 * RAPIDO/MEDIO/LENTO por días de cobertura.
 *
 * La consulta parte del CATÁLOGO (no de las ventas) a propósito: un producto
 * con stock y CERO ventas es justo el que el reporte debe delatar
 * (sin movimiento → capital inmovilizado). Se listan los artículos activos
 * con venta en el período O stock actual > 0.
 */
@Service
public class RotationReportService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public RotationReportService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.cajaCRUD = cajaCRUD;
    }

    private String resolveDb(int cajaCode) {
        String db = cajaCRUD.findById(cajaCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caja no encontrada"))
                .getNombreDb();
        if (db == null || !SAFE_DB.matcher(db).matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nombre de BD de caja inválido");
        }
        return db;
    }

    private List<Integer> resolveCajas(Integer caja) {
        if (caja != null) return List.of(caja);
        List<Integer> ids = new ArrayList<>();
        for (Caja c : cajaCRUD.findAll()) {
            if (c.getCodigo() != null && c.getNombreDb() != null) ids.add(c.getCodigo());
        }
        return ids;
    }

    public RotationReportResponse rotation(LocalDate from, LocalDate to, Integer caja, Integer bodega,
                                           List<String> excludeCategories,
                                           int umbralRapidoDias, int umbralMedioDias, Integer limit) {
        if (umbralRapidoDias <= 0 || umbralRapidoDias >= umbralMedioDias) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "umbralRapidoDias debe ser > 0 y menor que umbralMedioDias");
        }
        long dias = ChronoUnit.DAYS.between(from, to) + 1;
        if (dias <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido");
        }
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) {
            return new RotationReportResponse(from, to, caja, bodega, dias,
                    umbralRapidoDias, umbralMedioDias, List.of(), List.of());
        }

        // Ventas del período: UNION ALL por caja, args (from, to) repetidos.
        List<String> parts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (int c : cajas) {
            String db = resolveDb(c);
            parts.add(" SELECT d.codigo_articulo, d.cantidad, d.total "
                    + " FROM " + db + ".detalle_factura d "
                    + " JOIN " + db + ".encabezado_factura e ON e.numero_factura = d.numero_factura "
                    + " WHERE e.estado_factura <> 'NULA' AND e.fecha >= ? AND e.fecha < ? ");
            args.add(from.atStartOfDay());
            args.add(to.plusDays(1).atStartOfDay());
        }

        // Orden posicional de args = orden textual del SQL: primero la
        // subquery de ventas (fechas por caja), luego bodega, luego exclusión.
        StringBuilder sql = new StringBuilder(
                "SELECT a.codigo_articulo, a.articulo, a.codigo_marca, m.descripcion AS categoria, "
                + " IFNULL(v.unidades, 0) AS unidades, IFNULL(v.venta, 0) AS venta, "
                + " IFNULL(s.stock, 0) AS stock "
                + " FROM admin_tools.articulo a "
                + " LEFT JOIN admin_tools.marcas m ON m.codigo_marca = a.codigo_marca "
                + " LEFT JOIN (SELECT u.codigo_articulo, SUM(u.cantidad) AS unidades, SUM(u.total) AS venta "
                + "            FROM (" + String.join(" UNION ALL ", parts) + ") u "
                + "            GROUP BY u.codigo_articulo) v ON v.codigo_articulo = a.codigo_articulo "
                + " LEFT JOIN (SELECT codigo_articulo, SUM(cantidad) AS stock "
                + "            FROM admin_tools.existencia_articulo_bodega ");
        if (bodega != null) {
            sql.append(" WHERE codigo_bodega = ? ");
            args.add(bodega);
        }
        sql.append("            GROUP BY codigo_articulo) s ON s.codigo_articulo = a.codigo_articulo "
                 + " WHERE a.estado = 1 AND (IFNULL(v.unidades, 0) > 0 OR IFNULL(s.stock, 0) > 0) ");
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            sql.append(" AND (m.descripcion IS NULL OR m.descripcion NOT IN (")
               .append(String.join(",", excludeCategories.stream().map(x -> "?").toList()))
               .append(")) ");
            args.addAll(excludeCategories);
        }
        sql.append(" ORDER BY unidades DESC, stock DESC ");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());

        BigDecimal diasBd = new BigDecimal(dias);
        List<RotationItem> items = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            BigDecimal unidades = bd(r, "unidades");
            BigDecimal stock = bd(r, "stock");
            BigDecimal ventaDiaria = unidades.divide(diasBd, 4, RoundingMode.HALF_UP);
            boolean sinMovimiento = unidades.signum() == 0;

            BigDecimal cobertura = null;
            if (ventaDiaria.signum() > 0) {
                cobertura = stock.divide(ventaDiaria, 1, RoundingMode.HALF_UP);
            }
            BigDecimal rotacion = null;
            if (stock.signum() > 0) {
                rotacion = unidades.divide(stock, 2, RoundingMode.HALF_UP);
            }
            // Sin ventas → cobertura infinita → LENTO. Con ventas, por días de
            // cobertura (stock 0 con ventas = cobertura 0 = RAPIDO: vuela).
            String clase;
            if (sinMovimiento) {
                clase = "LENTO";
            } else if (cobertura.compareTo(new BigDecimal(umbralRapidoDias)) <= 0) {
                clase = "RAPIDO";
            } else if (cobertura.compareTo(new BigDecimal(umbralMedioDias)) <= 0) {
                clase = "MEDIO";
            } else {
                clase = "LENTO";
            }
            Number codigo = (Number) r.get("codigo_articulo");
            items.add(new RotationItem(
                    codigo != null ? codigo.intValue() : 0,
                    (String) r.get("articulo"),
                    r.get("codigo_marca") != null ? ((Number) r.get("codigo_marca")).intValue() : null,
                    (String) r.get("categoria"),
                    unidades, bd(r, "venta"), stock, ventaDiaria, cobertura, rotacion,
                    sinMovimiento, clase));
        }

        List<RotationClassSummary> resumen = resumir(items);
        if (limit != null && limit > 0 && items.size() > limit) {
            items = items.subList(0, limit);
        }
        return new RotationReportResponse(from, to, caja, bodega, dias,
                umbralRapidoDias, umbralMedioDias, resumen, items);
    }

    /** Resumen por clasificación (siempre sobre la lista COMPLETA, antes del limit). */
    private List<RotationClassSummary> resumir(List<RotationItem> items) {
        Map<String, List<RotationItem>> porClase = new LinkedHashMap<>();
        porClase.put("RAPIDO", new ArrayList<>());
        porClase.put("MEDIO", new ArrayList<>());
        porClase.put("LENTO", new ArrayList<>());
        for (RotationItem it : items) {
            porClase.get(it.clasificacion()).add(it);
        }
        List<RotationClassSummary> resumen = new ArrayList<>(3);
        for (Map.Entry<String, List<RotationItem>> e : porClase.entrySet()) {
            BigDecimal unidades = e.getValue().stream()
                    .map(RotationItem::unidades).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal stock = e.getValue().stream()
                    .map(RotationItem::stockActual).reduce(BigDecimal.ZERO, BigDecimal::add);
            int sinMov = (int) e.getValue().stream().filter(RotationItem::sinMovimiento).count();
            resumen.add(new RotationClassSummary(e.getKey(), e.getValue().size(), sinMov, unidades, stock));
        }
        return resumen;
    }

    private static BigDecimal bd(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        return new BigDecimal(v.toString());
    }
}

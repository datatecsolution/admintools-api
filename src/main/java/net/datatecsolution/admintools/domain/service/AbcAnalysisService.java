package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.AbcClassSummary;
import net.datatecsolution.admintools.domain.dto.AbcItem;
import net.datatecsolution.admintools.domain.dto.AbcReportResponse;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-062 — análisis ABC de productos: venta por artículo en un rango de
 * fechas, consolidando TODAS las cajas registradas (o una elegida), con
 * clasificación por participación acumulada sobre la venta total.
 *
 * Sigue el patrón cross-DB de {@link InvoiceAdminQueryService}: UNION ALL de
 * {@code <caja>.detalle_factura ⋈ <caja>.encabezado_factura} sobre el
 * commonDataSource (mismo servidor MySQL), agregado server-side y catálogo
 * desde {@code admin_tools.articulo}/{@code marcas}.
 *
 * Metodología validada con datos reales (docs/reportes-dulce-morena del repo
 * Swing): la categoría de negocio es la tabla {@code marcas}; se filtran las
 * facturas NULA; algunas categorías internas (p.ej. TECNO en dulce) son ruido
 * y se excluyen por parámetro, nunca hardcodeadas.
 */
@Service
public class AbcAnalysisService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public AbcAnalysisService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
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

    /** caja != null → esa caja; null → TODAS las cajas registradas (consolidado). */
    private List<Integer> resolveCajas(Integer caja) {
        if (caja != null) return List.of(caja);
        List<Integer> ids = new ArrayList<>();
        for (Caja c : cajaCRUD.findAll()) {
            if (c.getCodigo() != null && c.getNombreDb() != null) ids.add(c.getCodigo());
        }
        return ids;
    }

    public AbcReportResponse abc(LocalDate from, LocalDate to, Integer caja,
                                 List<String> excludeCategories,
                                 BigDecimal umbralA, BigDecimal umbralB, Integer limit) {
        if (umbralA.compareTo(umbralB) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "umbralA debe ser menor que umbralB");
        }
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) {
            return new AbcReportResponse(from, to, caja, umbralA, umbralB,
                    BigDecimal.ZERO, List.of(), List.of());
        }

        // UNION ALL por caja — mismos args de fecha repetidos por caja (orden
        // posicional). Rango semiabierto [from, to+1) como el resto del repo.
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

        // LEFT JOIN al catálogo: un artículo borrado del catálogo no puede
        // desaparecer la venta del análisis (misma filosofía que US-147).
        StringBuilder sql = new StringBuilder(
                "SELECT u.codigo_articulo, a.articulo, a.codigo_marca, m.descripcion AS categoria, "
                + " SUM(u.cantidad) AS unidades, SUM(u.total) AS venta "
                + " FROM (" + String.join(" UNION ALL ", parts) + ") u "
                + " LEFT JOIN admin_tools.articulo a ON a.codigo_articulo = u.codigo_articulo "
                + " LEFT JOIN admin_tools.marcas m ON m.codigo_marca = a.codigo_marca "
                + " WHERE 1=1 ");
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            sql.append(" AND (m.descripcion IS NULL OR m.descripcion NOT IN (")
               .append(String.join(",", excludeCategories.stream().map(x -> "?").toList()))
               .append(")) ");
            args.addAll(excludeCategories);
        }
        sql.append(" GROUP BY u.codigo_articulo, a.articulo, a.codigo_marca, m.descripcion "
                 + " ORDER BY venta DESC ");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());

        BigDecimal totalVenta = rows.stream()
                .map(r -> bd(r, "venta"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Clasificación en Java sobre la lista ya ordenada. La clase se decide
        // por el acumulado ANTES del ítem: el producto que cruza un umbral
        // pertenece a la clase de abajo (criterio estándar; si no, el líder de
        // venta de un catálogo chico caería en C por acumular 100% él solo).
        List<AbcItem> items = new ArrayList<>(rows.size());
        BigDecimal acumulado = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            BigDecimal venta = bd(r, "venta");
            BigDecimal pctAntes = pct(acumulado, totalVenta);
            acumulado = acumulado.add(venta);
            BigDecimal participacion = pct(venta, totalVenta);
            BigDecimal pctAcumulado = pct(acumulado, totalVenta);
            String clase = pctAntes.compareTo(umbralA) < 0 ? "A"
                    : pctAntes.compareTo(umbralB) < 0 ? "B" : "C";
            Number codigo = (Number) r.get("codigo_articulo");
            items.add(new AbcItem(
                    codigo != null ? codigo.intValue() : 0,
                    r.get("articulo") != null ? (String) r.get("articulo") : "(artículo eliminado)",
                    r.get("codigo_marca") != null ? ((Number) r.get("codigo_marca")).intValue() : null,
                    (String) r.get("categoria"),
                    bd(r, "unidades"),
                    venta,
                    participacion,
                    pctAcumulado,
                    clase));
        }

        List<AbcClassSummary> resumen = resumir(items, totalVenta);
        if (limit != null && limit > 0 && items.size() > limit) {
            items = items.subList(0, limit);
        }
        return new AbcReportResponse(from, to, caja, umbralA, umbralB, totalVenta, resumen, items);
    }

    /** Resumen por clase (siempre sobre la lista COMPLETA, antes del limit). */
    private List<AbcClassSummary> resumir(List<AbcItem> items, BigDecimal totalVenta) {
        Map<String, List<AbcItem>> porClase = new LinkedHashMap<>();
        porClase.put("A", new ArrayList<>());
        porClase.put("B", new ArrayList<>());
        porClase.put("C", new ArrayList<>());
        for (AbcItem it : items) {
            porClase.get(it.clase()).add(it);
        }
        List<AbcClassSummary> resumen = new ArrayList<>(3);
        for (Map.Entry<String, List<AbcItem>> e : porClase.entrySet()) {
            BigDecimal venta = e.getValue().stream()
                    .map(AbcItem::venta).reduce(BigDecimal.ZERO, BigDecimal::add);
            resumen.add(new AbcClassSummary(e.getKey(), e.getValue().size(), venta, pct(venta, totalVenta)));
        }
        return resumen;
    }

    private static BigDecimal pct(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) return BigDecimal.ZERO;
        return parte.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        return new BigDecimal(v.toString());
    }
}

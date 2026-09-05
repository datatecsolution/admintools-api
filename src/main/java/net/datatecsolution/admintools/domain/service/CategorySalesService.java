package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CategoryProjection;
import net.datatecsolution.admintools.domain.dto.CategorySalesResponse;
import net.datatecsolution.admintools.domain.dto.CategorySalesRow;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-106 — ventas por categoría con comparativo trimestral y proyección de
 * cierre. Deriva del análisis real de Dulce Morena
 * (docs/reportes-dulce-morena del repo Swing): la categoría del negocio es
 * la tabla {@code marcas}, las facturas NULA se excluyen, y las categorías
 * ruido (p.ej. TECNO) se excluyen POR PARÁMETRO.
 *
 * Proyección de los trimestres restantes del año en curso, por categoría,
 * con la metodología del Excel validado (ver {@link CategoryProjection}):
 * factor interanual YTD → run-rate del parcial → repetir temporada.
 */
@Service
public class CategorySalesService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public CategorySalesService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
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

    /** Clave de una celda del comparativo. */
    private record Celda(String categoria, int anio, int trimestre) {}

    public CategorySalesResponse categorySales(int fromYear, LocalDate hasta, Integer caja,
                                               List<String> excludeCategories) {
        int anioActual = hasta.getYear();
        if (fromYear > anioActual) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromYear no puede ser futuro");
        }
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) {
            return new CategorySalesResponse(fromYear, hasta, caja, List.of(), List.of());
        }

        // ---- Query 1: comparativo YEAR×QUARTER×categoría desde fromYear ----
        List<CategorySalesRow> filas = filasTrimestrales(cajas, LocalDate.of(fromYear, 1, 1),
                hasta.plusDays(1), excludeCategories);

        // ---- Query 2: YTD del año ANTERIOR al mismo día del año (para el factor) ----
        LocalDate inicioPrev = LocalDate.of(anioActual - 1, 1, 1);
        // mismo día del año anterior (29-feb cae a 28-feb)
        LocalDate cortePrev = hasta.minusYears(1);
        Map<String, BigDecimal[]> ytdPrev = ytdPorCategoria(cajas, inicioPrev, cortePrev.plusDays(1), excludeCategories);

        // ---- Proyección ----
        Map<Celda, CategorySalesRow> mapa = new HashMap<>();
        Map<String, Integer> codigoPorCat = new HashMap<>();
        Map<String, BigDecimal[]> ytdActual = new LinkedHashMap<>();
        for (CategorySalesRow f : filas) {
            mapa.put(new Celda(f.categoria(), f.anio(), f.trimestre()), f);
            codigoPorCat.putIfAbsent(f.categoria(), f.codigoCategoria());
            if (f.anio() == anioActual) {
                BigDecimal[] acc = ytdActual.computeIfAbsent(f.categoria(),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                acc[0] = acc[0].add(f.unidades());
                acc[1] = acc[1].add(f.venta());
            }
        }

        int triActual = (hasta.getMonthValue() - 1) / 3 + 1;
        LocalDate iniTri = LocalDate.of(anioActual, (triActual - 1) * 3 + 1, 1);
        LocalDate finTri = iniTri.plusMonths(3);
        long diasTri = finTri.toEpochDay() - iniTri.toEpochDay();
        long diasTranscurridos = hasta.toEpochDay() - iniTri.toEpochDay() + 1;

        List<CategoryProjection> proyecciones = new ArrayList<>();
        for (String cat : ytdActual.keySet()) {
            BigDecimal factorVenta = factor(ytdActual.get(cat), ytdPrev.get(cat), 1);
            BigDecimal factorUnid = factor(ytdActual.get(cat), ytdPrev.get(cat), 0);
            for (int tri = triActual; tri <= 4; tri++) {
                CategorySalesRow basePrev = mapa.get(new Celda(cat, anioActual - 1, tri));
                CategorySalesRow parcial = mapa.get(new Celda(cat, anioActual, tri));
                BigDecimal venta = null;
                BigDecimal unidades = null;
                String metodo = null;
                if (factorVenta != null && basePrev != null) {
                    venta = basePrev.venta().multiply(factorVenta);
                    unidades = factorUnid != null ? basePrev.unidades().multiply(factorUnid) : null;
                    metodo = "FACTOR_YOY";
                } else if (tri == triActual && parcial != null && diasTranscurridos > 0) {
                    BigDecimal escala = BigDecimal.valueOf(diasTri)
                            .divide(BigDecimal.valueOf(diasTranscurridos), 6, RoundingMode.HALF_UP);
                    venta = parcial.venta().multiply(escala);
                    unidades = parcial.unidades().multiply(escala);
                    metodo = "RUN_RATE";
                } else if (basePrev != null) {
                    venta = basePrev.venta();
                    unidades = basePrev.unidades();
                    metodo = "ANIO_ANTERIOR";
                }
                if (metodo != null) {
                    proyecciones.add(new CategoryProjection(anioActual, tri, codigoPorCat.get(cat), cat,
                            unidades != null ? unidades.setScale(0, RoundingMode.HALF_UP) : null,
                            venta.setScale(2, RoundingMode.HALF_UP), metodo));
                }
            }
        }

        return new CategorySalesResponse(fromYear, hasta, caja, filas, proyecciones);
    }

    /** factor YTD actual / YTD anterior; null si falta base o la base es 0. */
    private static BigDecimal factor(BigDecimal[] actual, BigDecimal[] prev, int idx) {
        if (actual == null || prev == null || prev[idx].signum() <= 0 || actual[idx].signum() <= 0) {
            return null;
        }
        return actual[idx].divide(prev[idx], 6, RoundingMode.HALF_UP);
    }

    private List<CategorySalesRow> filasTrimestrales(List<Integer> cajas, LocalDate desde, LocalDate hastaExcl,
                                                     List<String> excludeCategories) {
        List<String> parts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (int c : cajas) {
            String db = resolveDb(c);
            parts.add(" SELECT e.fecha, d.codigo_articulo, d.cantidad, d.total "
                    + " FROM " + db + ".detalle_factura d "
                    + " JOIN " + db + ".encabezado_factura e ON e.numero_factura = d.numero_factura "
                    + " WHERE e.estado_factura <> 'NULA' AND e.fecha >= ? AND e.fecha < ? ");
            args.add(desde.atStartOfDay());
            args.add(hastaExcl.atStartOfDay());
        }
        StringBuilder sql = new StringBuilder(
                "SELECT YEAR(u.fecha) AS anio, QUARTER(u.fecha) AS trimestre, "
                + " a.codigo_marca, IFNULL(m.descripcion, '(sin categoría)') AS categoria, "
                + " SUM(u.cantidad) AS unidades, SUM(u.total) AS venta "
                + " FROM (" + String.join(" UNION ALL ", parts) + ") u "
                + " LEFT JOIN admin_tools.articulo a ON a.codigo_articulo = u.codigo_articulo "
                + " LEFT JOIN admin_tools.marcas m ON m.codigo_marca = a.codigo_marca "
                + " WHERE 1=1 ");
        appendExclusion(sql, args, excludeCategories);
        sql.append(" GROUP BY anio, trimestre, a.codigo_marca, categoria ORDER BY anio, trimestre, venta DESC ");

        List<CategorySalesRow> filas = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(sql.toString(), args.toArray())) {
            filas.add(new CategorySalesRow(
                    ((Number) r.get("anio")).intValue(),
                    ((Number) r.get("trimestre")).intValue(),
                    r.get("codigo_marca") != null ? ((Number) r.get("codigo_marca")).intValue() : null,
                    (String) r.get("categoria"),
                    bd(r, "unidades"), bd(r, "venta")));
        }
        return filas;
    }

    /** YTD por categoría en [desde, hastaExcl): [unidades, venta]. */
    private Map<String, BigDecimal[]> ytdPorCategoria(List<Integer> cajas, LocalDate desde, LocalDate hastaExcl,
                                                      List<String> excludeCategories) {
        List<String> parts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (int c : cajas) {
            String db = resolveDb(c);
            parts.add(" SELECT d.codigo_articulo, d.cantidad, d.total "
                    + " FROM " + db + ".detalle_factura d "
                    + " JOIN " + db + ".encabezado_factura e ON e.numero_factura = d.numero_factura "
                    + " WHERE e.estado_factura <> 'NULA' AND e.fecha >= ? AND e.fecha < ? ");
            args.add(desde.atStartOfDay());
            args.add(hastaExcl.atStartOfDay());
        }
        StringBuilder sql = new StringBuilder(
                "SELECT IFNULL(m.descripcion, '(sin categoría)') AS categoria, "
                + " SUM(u.cantidad) AS unidades, SUM(u.total) AS venta "
                + " FROM (" + String.join(" UNION ALL ", parts) + ") u "
                + " LEFT JOIN admin_tools.articulo a ON a.codigo_articulo = u.codigo_articulo "
                + " LEFT JOIN admin_tools.marcas m ON m.codigo_marca = a.codigo_marca "
                + " WHERE 1=1 ");
        appendExclusion(sql, args, excludeCategories);
        sql.append(" GROUP BY categoria ");

        Map<String, BigDecimal[]> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(sql.toString(), args.toArray())) {
            out.put((String) r.get("categoria"), new BigDecimal[]{bd(r, "unidades"), bd(r, "venta")});
        }
        return out;
    }

    private static void appendExclusion(StringBuilder sql, List<Object> args, List<String> excludeCategories) {
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            sql.append(" AND (m.descripcion IS NULL OR m.descripcion NOT IN (")
               .append(String.join(",", excludeCategories.stream().map(x -> "?").toList()))
               .append(")) ");
            args.addAll(excludeCategories);
        }
    }

    private static BigDecimal bd(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        return new BigDecimal(v.toString());
    }
}

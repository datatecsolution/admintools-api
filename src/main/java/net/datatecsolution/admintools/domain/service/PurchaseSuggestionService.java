package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionItem;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionsResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseUrgencySummary;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-064 — proyección de necesidad de compras: con la venta diaria promedio
 * del período base (default últimos 30 días, todas las cajas), el stock
 * actual (saldo materializado) y el mínimo configurado
 * ({@code articulo_kardex.cantidad_minima}, la misma fuente del low-stock),
 * sugiere cuánto comprar para cubrir un horizonte de días sin caer bajo el
 * mínimo:
 *
 * <pre>sugerido = max(0, ventaDiaria × horizonteDias + minimo − stockActual)</pre>
 *
 * Urgencia: CRITICA = stock en o bajo el mínimo (o agotado con venta),
 * ALTA = cobertura ≤ 7 días, NORMAL = el resto con compra sugerida.
 */
@Service
public class PurchaseSuggestionService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");
    private static final BigDecimal SIETE_DIAS = new BigDecimal(7);

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public PurchaseSuggestionService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
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

    public PurchaseSuggestionsResponse suggestions(LocalDate from, LocalDate to, Integer caja, Integer bodega,
                                                   List<String> excludeCategories,
                                                   int horizonteDias, boolean incluirTodos, Integer limit) {
        if (horizonteDias <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horizonteDias debe ser > 0");
        }
        long dias = ChronoUnit.DAYS.between(from, to) + 1;
        if (dias <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido");
        }
        List<Integer> cajas = resolveCajas(caja);
        if (cajas.isEmpty()) {
            return new PurchaseSuggestionsResponse(from, to, caja, bodega, dias,
                    horizonteDias, List.of(), List.of());
        }

        List<String> parts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (int c : cajas) {
            String db = resolveDb(c);
            parts.add(" SELECT d.codigo_articulo, d.cantidad "
                    + " FROM " + db + ".detalle_factura d "
                    + " JOIN " + db + ".encabezado_factura e ON e.numero_factura = d.numero_factura "
                    + " WHERE e.estado_factura <> 'NULA' AND e.fecha >= ? AND e.fecha < ? ");
            args.add(from.atStartOfDay());
            args.add(to.plusDays(1).atStartOfDay());
        }

        // Orden posicional = orden textual: ventas (fechas por caja), bodega
        // del stock, bodega del mínimo, categorías excluidas.
        StringBuilder sql = new StringBuilder(
                "SELECT a.codigo_articulo, a.articulo, a.codigo_marca, m.descripcion AS categoria, "
                + " IFNULL(v.unidades, 0) AS unidades, IFNULL(s.stock, 0) AS stock, IFNULL(k.minimo, 0) AS minimo "
                + " FROM admin_tools.articulo a "
                + " LEFT JOIN admin_tools.marcas m ON m.codigo_marca = a.codigo_marca "
                + " LEFT JOIN (SELECT u.codigo_articulo, SUM(u.cantidad) AS unidades "
                + "            FROM (" + String.join(" UNION ALL ", parts) + ") u "
                + "            GROUP BY u.codigo_articulo) v ON v.codigo_articulo = a.codigo_articulo "
                + " LEFT JOIN (SELECT codigo_articulo, SUM(cantidad) AS stock "
                + "            FROM admin_tools.existencia_articulo_bodega ");
        if (bodega != null) {
            sql.append(" WHERE codigo_bodega = ? ");
            args.add(bodega);
        }
        sql.append("            GROUP BY codigo_articulo) s ON s.codigo_articulo = a.codigo_articulo "
                 + " LEFT JOIN (SELECT codigo_articulo, SUM(cantidad_minima) AS minimo "
                 + "            FROM admin_tools.articulo_kardex ");
        if (bodega != null) {
            sql.append(" WHERE codigo_bodega = ? ");
            args.add(bodega);
        }
        sql.append("            GROUP BY codigo_articulo) k ON k.codigo_articulo = a.codigo_articulo "
                 + " WHERE a.estado = 1 "
                 + " AND (IFNULL(v.unidades, 0) > 0 OR IFNULL(s.stock, 0) > 0 OR IFNULL(k.minimo, 0) > 0) ");
        if (excludeCategories != null && !excludeCategories.isEmpty()) {
            sql.append(" AND (m.descripcion IS NULL OR m.descripcion NOT IN (")
               .append(String.join(",", excludeCategories.stream().map(x -> "?").toList()))
               .append(")) ");
            args.addAll(excludeCategories);
        }

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());

        BigDecimal diasBd = new BigDecimal(dias);
        BigDecimal horizonte = new BigDecimal(horizonteDias);
        List<PurchaseSuggestionItem> items = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            BigDecimal unidades = bd(r, "unidades");
            BigDecimal stock = bd(r, "stock");
            BigDecimal minimo = bd(r, "minimo");
            BigDecimal ventaDiaria = unidades.divide(diasBd, 4, RoundingMode.HALF_UP);

            BigDecimal objetivo = ventaDiaria.multiply(horizonte).add(minimo);
            BigDecimal sugerido = objetivo.subtract(stock);
            if (sugerido.signum() < 0) sugerido = BigDecimal.ZERO;
            // hacia arriba y en enteros: una orden de compra no pide 3.2 uds
            sugerido = sugerido.setScale(0, RoundingMode.CEILING);

            if (!incluirTodos && sugerido.signum() == 0) continue;

            // stock negativo = anomalía de datos (sobreventa histórica): para
            // efectos de cobertura equivale a "no hay nada" → 0 días, no días
            // negativos que confunden el reporte.
            BigDecimal cobertura = null;
            if (ventaDiaria.signum() > 0) {
                cobertura = stock.signum() <= 0 ? BigDecimal.ZERO.setScale(1)
                        : stock.divide(ventaDiaria, 1, RoundingMode.HALF_UP);
            }

            // CRITICA: en o bajo el mínimo configurado, o agotado pero con
            // venta en el período (se está vendiendo sin stock).
            String urgencia;
            boolean bajoMinimo = minimo.signum() > 0 && stock.compareTo(minimo) <= 0;
            boolean agotadoConVenta = stock.signum() <= 0 && unidades.signum() > 0;
            if (bajoMinimo || agotadoConVenta) {
                urgencia = "CRITICA";
            } else if (cobertura != null && cobertura.compareTo(SIETE_DIAS) <= 0) {
                urgencia = "ALTA";
            } else {
                urgencia = "NORMAL";
            }
            Number codigo = (Number) r.get("codigo_articulo");
            items.add(new PurchaseSuggestionItem(
                    codigo != null ? codigo.intValue() : 0,
                    (String) r.get("articulo"),
                    r.get("codigo_marca") != null ? ((Number) r.get("codigo_marca")).intValue() : null,
                    (String) r.get("categoria"),
                    unidades, ventaDiaria, stock, minimo, cobertura, sugerido, urgencia));
        }

        // CRITICA → ALTA → NORMAL; dentro de cada urgencia, menor cobertura
        // primero (null = sin ventas, al final de su grupo).
        items.sort(Comparator
                .comparingInt((PurchaseSuggestionItem i) -> rank(i.urgencia()))
                .thenComparing(PurchaseSuggestionItem::diasCobertura,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        List<PurchaseUrgencySummary> resumen = resumir(items);
        if (limit != null && limit > 0 && items.size() > limit) {
            items = items.subList(0, limit);
        }
        return new PurchaseSuggestionsResponse(from, to, caja, bodega, dias,
                horizonteDias, resumen, items);
    }

    private static int rank(String urgencia) {
        return switch (urgencia) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            default -> 2;
        };
    }

    /** Resumen por urgencia (sobre la lista COMPLETA, antes del limit). */
    private List<PurchaseUrgencySummary> resumir(List<PurchaseSuggestionItem> items) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        conteo.put("CRITICA", 0);
        conteo.put("ALTA", 0);
        conteo.put("NORMAL", 0);
        for (PurchaseSuggestionItem it : items) {
            conteo.merge(it.urgencia(), 1, Integer::sum);
        }
        List<PurchaseUrgencySummary> resumen = new ArrayList<>(3);
        conteo.forEach((u, n) -> resumen.add(new PurchaseUrgencySummary(u, n)));
        return resumen;
    }

    private static BigDecimal bd(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        return new BigDecimal(v.toString());
    }
}

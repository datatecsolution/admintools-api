package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.SalesRankingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Ranking de ventas para ordenar el catálogo de facturación (US-094). Suma las
 * unidades vendidas por producto en la ventana configurada (config_app.
 * dias_ranking_mas_vendidos) sobre la BD de la caja actual (TenantContext) y
 * deriva el orden de categorías + el top de productos.
 *
 * Cross-DB como el cierre: detalle/encabezado en {@code <caja>} y la categoría
 * en {@code admin_tools.articulo}. Se cachea por caja+ventana ~1 h porque el
 * ranking cambia lento y la agregación no debe correr en cada carga.
 */
@Service
public class SalesRankingService {

    private static final Logger log = LoggerFactory.getLogger(SalesRankingService.class);
    private static final int TOP_PRODUCTS = 30;
    private static final long TTL_MS = 60 * 60 * 1000L; // 1 h
    // El nombre de la BD viene de la tabla cajas; aun así se valida antes de
    // concatenarlo en el SQL (defensa en profundidad).
    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final ConfigService configService;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(long at, SalesRankingResponse value) {}

    public SalesRankingService(@Qualifier("commonDataSource") DataSource commonDS,
                               ConfigService configService) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.configService = configService;
    }

    /** Ranking de la caja actual; days null = el configurado. */
    public SalesRankingResponse ranking(Integer daysOverride) {
        String caja = TenantContext.getTenant();
        int days = daysOverride != null && daysOverride > 0
                ? daysOverride : configService.getDiasRankingMasVendidos();
        if (caja == null || !SAFE_DB.matcher(caja).matches()) {
            return new SalesRankingResponse(List.of(), List.of(), days);
        }
        String key = caja + ":" + days;
        Cached c = cache.get(key);
        long now = System.currentTimeMillis();
        if (c != null && now - c.at() < TTL_MS) {
            return c.value();
        }
        SalesRankingResponse value = compute(caja, days);
        cache.put(key, new Cached(now, value));
        return value;
    }

    private SalesRankingResponse compute(String caja, int days) {
        String base = "FROM " + caja + ".detalle_factura d "
                + "JOIN " + caja + ".encabezado_factura e ON e.numero_factura = d.numero_factura "
                + "JOIN admin_tools.articulo a ON a.codigo_articulo = d.codigo_articulo "
                + "WHERE e.fecha >= (NOW() - INTERVAL ? DAY) AND e.estado_factura = 'ACT' ";
        try {
            // Top productos por unidades (para el chip "Más vendidos").
            List<Integer> topProducts = jdbc.query(
                    "SELECT d.codigo_articulo AS pid, SUM(d.cantidad) AS u " + base
                            + "GROUP BY d.codigo_articulo ORDER BY u DESC LIMIT " + TOP_PRODUCTS,
                    (rs, i) -> rs.getInt("pid"), days);
            // Categorías por unidades vendidas.
            List<Integer> categories = jdbc.query(
                    "SELECT a.codigo_marca AS cat, SUM(d.cantidad) AS u " + base
                            + "GROUP BY a.codigo_marca ORDER BY u DESC",
                    (rs, i) -> rs.getInt("cat"), days);
            return new SalesRankingResponse(categories, topProducts, days);
        } catch (RuntimeException e) {
            log.warn("Ranking de ventas falló para caja {} ({} días): {}", caja, days, e.getMessage());
            return new SalesRankingResponse(List.of(), List.of(), days);
        }
    }
}

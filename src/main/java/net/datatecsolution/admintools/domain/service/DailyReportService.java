package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CajaDailyReport;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-047 — reporte diario consolidado para el panel admin: ventas, desglose
 * por método de pago (efectivo/tarjeta/crédito), desglose por tasa,
 * descuentos y anulaciones, por caja y consolidado.
 *
 * Mismo patrón cross-caja que InvoiceAdminQueryService (query directa a
 * <db_caja>.encabezado_factura, nombre de BD validado contra inyección) y
 * mismas fórmulas de método de pago que CierreCajaService.computeCuadre():
 * SUM(cobro_efectivo), SUM(cobro_tarjeta) y crédito = SUM(total) de las
 * tipo_factura=2 — pero SIN filtrar por usuario/turno: el corte es el DÍA
 * calendario completo (fecha >= date AND fecha < date+1).
 */
@Service
public class DailyReportService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public DailyReportService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.cajaCRUD = cajaCRUD;
    }

    public DailyReportResponse daily(LocalDate date, Integer caja) {
        List<Caja> cajas = resolveCajas(caja);
        List<CajaDailyReport> filas = new ArrayList<>();
        for (Caja c : cajas) {
            filas.add(reportForCaja(c, date));
        }
        return new DailyReportResponse(date, filas, consolidar(filas));
    }

    private List<Caja> resolveCajas(Integer caja) {
        if (caja != null) {
            Caja c = cajaCRUD.findById(caja)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Caja no encontrada"));
            return List.of(c);
        }
        return cajaCRUD.findAll().stream()
                .filter(c -> c.getCodigo() != null && c.getNombreDb() != null)
                .toList();
    }

    private CajaDailyReport reportForCaja(Caja caja, LocalDate date) {
        String db = caja.getNombreDb();
        if (db == null || !SAFE_DB.matcher(db).matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nombre de BD de caja inválido");
        }
        Object desde = date.atStartOfDay();
        Object hasta = date.plusDays(1).atStartOfDay();

        Map<String, Object> act = jdbc.queryForMap(
                "SELECT COUNT(*) AS ventas, IFNULL(SUM(total),0) AS total, "
                + " IFNULL(SUM(cobro_efectivo),0) AS efectivo, IFNULL(SUM(cobro_tarjeta),0) AS tarjeta, "
                + " IFNULL(SUM(CASE WHEN tipo_factura = 2 THEN total ELSE 0 END),0) AS credito, "
                + " IFNULL(SUM(descuento),0) AS descuentos, "
                + " IFNULL(SUM(subtotal_excento),0) AS exento, "
                + " IFNULL(SUM(subtotal15),0) AS base15, IFNULL(SUM(subtotal18),0) AS base18, "
                + " IFNULL(SUM(impuesto),0) AS isv15, IFNULL(SUM(isv18),0) AS isv18 "
                + " FROM " + db + ".encabezado_factura "
                + " WHERE fecha >= ? AND fecha < ? AND estado_factura = 'ACT'",
                desde, hasta);
        Map<String, Object> nulas = jdbc.queryForMap(
                "SELECT COUNT(*) AS anuladas, IFNULL(SUM(total),0) AS total_anulado "
                + " FROM " + db + ".encabezado_factura "
                + " WHERE fecha >= ? AND fecha < ? AND estado_factura <> 'ACT'",
                desde, hasta);

        return new CajaDailyReport(caja.getCodigo(), caja.getDescripcion(),
                ((Number) act.get("ventas")).longValue(), bd(act, "total"),
                bd(act, "efectivo"), bd(act, "tarjeta"), bd(act, "credito"),
                bd(act, "descuentos"), bd(act, "exento"),
                bd(act, "base15"), bd(act, "base18"), bd(act, "isv15"), bd(act, "isv18"),
                ((Number) nulas.get("anuladas")).longValue(), bd(nulas, "total_anulado"));
    }

    private CajaDailyReport consolidar(List<CajaDailyReport> filas) {
        long ventas = 0;
        long anuladas = 0;
        BigDecimal total = BigDecimal.ZERO, efectivo = BigDecimal.ZERO, tarjeta = BigDecimal.ZERO,
                credito = BigDecimal.ZERO, descuentos = BigDecimal.ZERO, exento = BigDecimal.ZERO,
                base15 = BigDecimal.ZERO, base18 = BigDecimal.ZERO, isv15 = BigDecimal.ZERO,
                isv18 = BigDecimal.ZERO, totalAnulado = BigDecimal.ZERO;
        for (CajaDailyReport f : filas) {
            ventas += f.ventas();
            anuladas += f.anuladas();
            total = total.add(f.total());
            efectivo = efectivo.add(f.efectivo());
            tarjeta = tarjeta.add(f.tarjeta());
            credito = credito.add(f.credito());
            descuentos = descuentos.add(f.descuentos());
            exento = exento.add(f.exento());
            base15 = base15.add(f.base15());
            base18 = base18.add(f.base18());
            isv15 = isv15.add(f.isv15());
            isv18 = isv18.add(f.isv18());
            totalAnulado = totalAnulado.add(f.totalAnulado());
        }
        return new CajaDailyReport(0, "CONSOLIDADO", ventas, total, efectivo, tarjeta,
                credito, descuentos, exento, base15, base18, isv15, isv18,
                anuladas, totalAnulado);
    }

    private static BigDecimal bd(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}

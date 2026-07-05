package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.FiscalRangeRequest;
import net.datatecsolution.admintools.domain.dto.FiscalRangeResponse;
import net.datatecsolution.admintools.domain.dto.FiscalRangesResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * US-101 — CRUD de datos_factura (CAI/rangos fiscales) POR CAJA, replica de
 * CtlDatosFacturacion/DatosFacturacionDao del Swing. Opera cross-DB desde el
 * DataSource común (patrón InvoiceAdminQueryService) porque el admin elige
 * la caja en la UI — no depende del tenant de la sesión.
 *
 * Guardas (evolución de las del Swing, decisión de negocio 2026-07-05):
 *  - crear/actualizar: el rango NO debe SOLAPARSE con otro rango de la misma
 *    caja → 409. (Reemplaza al verificarFacturacionFactInicial del Swing,
 *    que comparaba contra el MAX(numero_factura) emitido y bloqueaba el
 *    escenario real de sobrepaso: el cliente se pasa del límite y el ente
 *    regulador le autoriza el rango nuevo DESDE el último autorizado, que
 *    queda por debajo de lo ya emitido.)
 *  - crear/actualizar: la numeración es CONDICIONAL — si factura_inicial >
 *    último numero_factura emitido, ALTER TABLE encabezado_factura
 *    AUTO_INCREMENT = factura_inicial (setNumeroFact, arranca el rango); si
 *    NO (sobrepaso), no se toca: la secuencia continúa donde está y las
 *    facturas ya emitidas conservan su rango anterior. La UI se lo explica
 *    al usuario con el ultimoNumero de FiscalRangesResponse.
 *  - eliminar: solo si ninguna factura usa el cod_rango
 *    (verificarFacturacionEliminacion) → 409 si está en uso.
 */
@Service
public class FiscalRangeService {

    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    @Autowired
    public FiscalRangeService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
        this(new JdbcTemplate(commonDS), cajaCRUD);
    }

    /** Para tests: permite inyectar el JdbcTemplate mockeado. */
    FiscalRangeService(JdbcTemplate jdbc, CajaCRUD cajaCRUD) {
        this.jdbc = jdbc;
        this.cajaCRUD = cajaCRUD;
    }

    public FiscalRangesResponse list(int cajaId) {
        String db = resolveDb(cajaId);
        int ultimo = ultimoNumero(db);
        return new FiscalRangesResponse(ultimo,
                jdbc.query(selectSql(db) + " ORDER BY df.codigo_rango DESC", rowMapper(ultimo)));
    }

    public FiscalRangeResponse get(int cajaId, int rangeId) {
        String db = resolveDb(cajaId);
        return requireRange(db, rangeId);
    }

    public FiscalRangeResponse create(int cajaId, FiscalRangeRequest request) {
        String db = resolveDb(cajaId);
        validate(request);
        checkNoOverlap(db, request.facturaInicial(), request.facturaFinal(), null);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + db + ".datos_factura"
                            + " (CAI, factura_inicial, factura_final, codigo_tipo_facturacion,"
                            + " cantida_solicitada, fecha_limite_emision, observacion)"
                            + " VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.cai().trim());
            ps.setInt(2, request.facturaInicial());
            ps.setInt(3, request.facturaFinal());
            ps.setString(4, request.codigoTipoFacturacion().trim());
            ps.setInt(5, request.cantidadSolicitada());
            ps.setDate(6, Date.valueOf(request.fechaLimiteEmision()));
            ps.setString(7, request.observacion() == null ? "" : request.observacion().trim());
            return ps;
        }, keyHolder);
        int id = Objects.requireNonNull(keyHolder.getKey()).intValue();

        setNumeroFactura(db, request.facturaInicial());
        return requireRange(db, id);
    }

    public FiscalRangeResponse update(int cajaId, int rangeId, FiscalRangeRequest request) {
        String db = resolveDb(cajaId);
        requireRange(db, rangeId);
        validate(request);
        checkNoOverlap(db, request.facturaInicial(), request.facturaFinal(), rangeId);

        jdbc.update("UPDATE " + db + ".datos_factura SET CAI=?, factura_inicial=?, factura_final=?,"
                        + " codigo_tipo_facturacion=?, cantida_solicitada=?, fecha_limite_emision=?,"
                        + " observacion=? WHERE codigo_rango=?",
                request.cai().trim(), request.facturaInicial(), request.facturaFinal(),
                request.codigoTipoFacturacion().trim(), request.cantidadSolicitada(),
                Date.valueOf(request.fechaLimiteEmision()),
                request.observacion() == null ? "" : request.observacion().trim(),
                rangeId);

        setNumeroFactura(db, request.facturaInicial());
        return requireRange(db, rangeId);
    }

    public void delete(int cajaId, int rangeId) {
        String db = resolveDb(cajaId);
        FiscalRangeResponse range = requireRange(db, rangeId);
        if (range.enUso()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: hay facturas emitidas con este rango");
        }
        jdbc.update("DELETE FROM " + db + ".datos_factura WHERE codigo_rango=?", rangeId);
    }

    // ================= guardas y helpers =================

    private void validate(FiscalRangeRequest request) {
        if (request.facturaFinal() < request.facturaInicial()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El número de factura final debe ser mayor o igual al inicial");
        }
    }

    /** Último número emitido en la caja (0 si nunca facturó). */
    private int ultimoNumero(String db) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(numero_factura), 0) FROM " + db + ".encabezado_factura", Integer.class);
        return max == null ? 0 : max;
    }

    /**
     * El rango [inicial, final] no debe solaparse con otro rango de la caja.
     * Los rangos legacy sin numeración parseable (inicial 'NA'→0) se ignoran.
     */
    private void checkNoOverlap(String db, int inicial, int fin, Integer excludeRangeId) {
        List<int[]> existentes = jdbc.query(
                "SELECT codigo_rango, factura_inicial, factura_final FROM " + db + ".datos_factura",
                (rs, i) -> new int[]{
                        rs.getInt("codigo_rango"),
                        parseIntOrZero(rs.getString("factura_inicial")),
                        parseIntOrZero(rs.getString("factura_final"))});
        for (int[] r : existentes) {
            if (excludeRangeId != null && r[0] == excludeRangeId) continue;
            if (r[1] <= 0) continue;
            boolean overlap = inicial <= r[2] && fin >= r[1];
            if (overlap) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El rango " + inicial + "–" + fin + " choca con el rango #" + r[0]
                                + " (" + r[1] + "–" + r[2] + ") de esta caja");
            }
        }
    }

    /**
     * Numeración condicional (espejo de setNumeroFact SOLO cuando corresponde):
     * si el rango arranca por encima de lo emitido, la próxima factura salta a
     * factura_inicial. Si el cliente YA se pasó (sobrepaso), no se toca nada:
     * la secuencia continúa donde está y las emitidas conservan su rango.
     * (InnoDB además ignora un AUTO_INCREMENT menor al máximo — no ejecutarlo
     * hace la intención explícita.)
     */
    private void setNumeroFactura(String db, int facturaInicial) {
        if (facturaInicial > ultimoNumero(db)) {
            jdbc.execute("ALTER TABLE " + db + ".encabezado_factura AUTO_INCREMENT = " + facturaInicial);
        }
    }

    private FiscalRangeResponse requireRange(String db, int rangeId) {
        List<FiscalRangeResponse> found = jdbc.query(
                selectSql(db) + " WHERE df.codigo_rango = ?", rowMapper(ultimoNumero(db)), rangeId);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el rango " + rangeId + " en la caja");
        }
        return found.get(0);
    }

    private String selectSql(String db) {
        return "SELECT df.codigo_rango, df.CAI, df.factura_inicial, df.factura_final,"
                + " df.codigo_tipo_facturacion, df.cantida_solicitada, df.fecha_limite_emision,"
                + " df.observacion,"
                + " EXISTS(SELECT 1 FROM " + db + ".encabezado_factura e"
                + "        WHERE e.cod_rango = df.codigo_rango) AS en_uso"
                + " FROM " + db + ".datos_factura df";
    }

    private RowMapper<FiscalRangeResponse> rowMapper(int ultimoNumero) {
        return (rs, i) -> {
            Integer inicial = parseIntOrZero(rs.getString("factura_inicial"));
            Integer fin = parseIntOrZero(rs.getString("factura_final"));
            return new FiscalRangeResponse(
                    rs.getInt("codigo_rango"),
                    rs.getString("CAI"),
                    inicial,
                    fin,
                    rs.getString("codigo_tipo_facturacion"),
                    rs.getInt("cantida_solicitada"),
                    rs.getDate("fecha_limite_emision") == null
                            ? null : rs.getDate("fecha_limite_emision").toLocalDate(),
                    rs.getString("observacion"),
                    usadas(inicial, fin, ultimoNumero),
                    rs.getBoolean("en_uso"));
        };
    }

    /**
     * Números CONSUMIDOS del rango: el último numero_factura de la caja
     * clampeado contra [inicial, final]. 0 para rangos futuros o legacy
     * sin numeración (inicial 'NA'→0).
     */
    private static long usadas(int inicial, int fin, int ultimoNumero) {
        if (inicial <= 0 || fin < inicial) return 0;
        return Math.max(0L, (long) Math.min(fin, ultimoNumero) - inicial + 1);
    }

    /** factura_inicial/final son varchar(11) legacy con default 'NA'. */
    private static Integer parseIntOrZero(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Nombre de BD de la caja, validado contra inyección (uso en SQL cross-DB). */
    private String resolveDb(int cajaId) {
        String db = cajaCRUD.findById(cajaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caja no encontrada"))
                .getNombreDb();
        if (db == null || !SAFE_DB.matcher(db).matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nombre de BD de caja inválido");
        }
        return db;
    }
}

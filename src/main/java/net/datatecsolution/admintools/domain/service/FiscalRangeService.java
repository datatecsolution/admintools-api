package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.FiscalRangeRequest;
import net.datatecsolution.admintools.domain.dto.FiscalRangeResponse;
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
 * Guardas del Swing:
 *  - crear/actualizar: factura_inicial > MAX(numero_factura) emitido en la
 *    caja (verificarFacturacionFactInicial) → 409 si no.
 *  - crear/actualizar: tras persistir, ALTER TABLE encabezado_factura
 *    AUTO_INCREMENT = factura_inicial (setNumeroFact) — es lo que hace que
 *    la próxima factura arranque el rango. Seguro: la guarda garantiza que
 *    el valor es mayor al máximo actual.
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

    public List<FiscalRangeResponse> list(int cajaId) {
        String db = resolveDb(cajaId);
        return jdbc.query(selectSql(db) + " ORDER BY df.codigo_rango DESC", rowMapper(ultimoNumero(db)));
    }

    public FiscalRangeResponse get(int cajaId, int rangeId) {
        String db = resolveDb(cajaId);
        return requireRange(db, rangeId);
    }

    public FiscalRangeResponse create(int cajaId, FiscalRangeRequest request) {
        String db = resolveDb(cajaId);
        validate(request);
        checkFacturaInicial(db, request.facturaInicial());

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
        checkFacturaInicial(db, request.facturaInicial());

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

    /** Espejo de verificarFacturacionFactInicial: no pisar numeración ya emitida. */
    private void checkFacturaInicial(String db, int facturaInicial) {
        int ultimo = ultimoNumero(db);
        if (facturaInicial <= ultimo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El número de factura inicial (" + facturaInicial
                            + ") debe ser mayor al último número emitido en la caja (" + ultimo + ")");
        }
    }

    /** Espejo de setNumeroFact: la próxima factura de la caja arranca el rango. */
    private void setNumeroFactura(String db, int facturaInicial) {
        jdbc.execute("ALTER TABLE " + db + ".encabezado_factura AUTO_INCREMENT = " + facturaInicial);
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

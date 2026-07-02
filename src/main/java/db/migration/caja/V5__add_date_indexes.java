package db.migration.caja;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Fase 5 (caja): Agregar indices en columnas de fecha para reportes.
 */
public class V5__add_date_indexes extends BaseJavaMigration {

    private static final String[][] INDEXES = {
        {"encabezado_factura", "fecha",             "idx_enc_fact_fecha"},
        {"encabezado_factura", "fecha_vencimiento", "idx_enc_fact_fecha_venc"},
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        for (String[] idx : INDEXES) {
            addIndexIfNotExists(conn, idx[0], idx[1], idx[2]);
        }
    }

    private void addIndexIfNotExists(Connection conn, String table, String column, String indexName)
            throws Exception {
        if (!columnExists(conn, table, column)) return;
        if (indexExistsOnColumn(conn, table, column)) return;

        try (Statement s = conn.createStatement()) {
            s.execute("ALTER TABLE `" + table + "` ADD INDEX `" + indexName + "` (`" + column + "`)");
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean indexExistsOnColumn(Connection conn, String table, String column) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? AND seq_in_index = 1")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}

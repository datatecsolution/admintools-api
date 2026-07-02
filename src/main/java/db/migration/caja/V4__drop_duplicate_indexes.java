package db.migration.caja;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Fase 4 (caja): Eliminar indice UNIQUE duplicado que replica la PK.
 */
public class V4__drop_duplicate_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        dropIndexIfExists(conn, "encabezado_factura", "numero_factura");
    }

    private void dropIndexIfExists(Connection conn, String table, String indexName) throws Exception {
        if (indexExists(conn, table, indexName)) {
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + table + "` DROP INDEX `" + indexName + "`");
            }
        }
    }

    private boolean indexExists(Connection conn, String table, String indexName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}

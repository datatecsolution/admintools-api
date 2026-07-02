package db.migration.caja;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V3: ALTERs condicionales para alinear cajas de clientes V1 con el esquema Sharon.
 */
public class V3__alter_columns_sharon extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        addColumn(conn, "encabezado_factura", "fecha_vencimiento", "date NOT NULL DEFAULT '1990-01-01'");
        addColumn(conn, "datos_factura", "observacion", "varchar(255) DEFAULT ''");
    }

    private void addColumn(Connection conn, String table, String column, String definition) throws Exception {
        if (!columnExists(conn, table, column)) {
            try (Statement s = conn.createStatement()) {
                s.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
            }
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
}

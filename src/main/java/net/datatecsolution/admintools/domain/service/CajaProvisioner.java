package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantRegistry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

/**
 * US-101 — Provisioning fisico de la BD de una caja nueva. Replica el flujo
 * del Swing (CajaDao.registrar + SchemaMigrator.migrateNewCajaDatabase):
 * CREATE DATABASE + Flyway db/migration/caja (V1..V8) parametrizada con la
 * caja y su bodega. Separado de CajaAdminService para poder mockearlo en tests.
 *
 * El espejo de migraciones vive en src/main/resources/db/migration/caja (SQL)
 * y src/main/java/db/migration/caja (Java) — ver README del espejo: toda
 * migracion de caja nueva en el Swing debe copiarse aqui.
 */
@Component
public class CajaProvisioner {

    private static final Logger log = LoggerFactory.getLogger(CajaProvisioner.class);

    private final JdbcTemplate commonJdbc;
    private final TenantRegistry tenantRegistry;

    public CajaProvisioner(@Qualifier("commonDataSource") DataSource commonDataSource,
                           TenantRegistry tenantRegistry) {
        this.commonJdbc = new JdbcTemplate(commonDataSource);
        this.tenantRegistry = tenantRegistry;
    }

    /** CREATE DATABASE. El nombre es derivado (admin_tools_caja_N), no input del usuario. */
    public void createDatabase(String nombreDb) {
        commonJdbc.execute("CREATE DATABASE `" + nombreDb + "`");
        log.info("BD de caja creada: {}", nombreDb);
    }

    /**
     * Espejo exacto de SchemaMigrator.runFlyway del Swing: misma tabla
     * schema_version, baseline 1, outOfOrder y placeholders caja_db /
     * codigo_bodega. Asi un Swing que arranque despues ve un historial
     * consistente (su repair()+migrate() no re-aplica nada).
     */
    public void migrate(String nombreDb, int codigoBodega) {
        Flyway flyway = Flyway.configure()
                .dataSource(tenantRegistry.jdbcUrlFor(nombreDb),
                        tenantRegistry.username(), tenantRegistry.password())
                .locations("classpath:db/migration/caja")
                .table("schema_version")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("baseline")
                .outOfOrder(true)
                .placeholders(Map.of(
                        "caja_db", nombreDb,
                        "codigo_bodega", String.valueOf(codigoBodega)))
                .load();
        flyway.repair();
        flyway.migrate();
        log.info("Migraciones de caja aplicadas en {} (bodega {})", nombreDb, codigoBodega);
    }

    /** Compensacion: solo se invoca sobre la BD recien creada en este request. */
    public void dropDatabase(String nombreDb) {
        commonJdbc.execute("DROP DATABASE IF EXISTS `" + nombreDb + "`");
        log.warn("BD de caja eliminada por compensacion: {}", nombreDb);
    }

    /** Alta en caliente del DataSource tenant de la caja nueva. */
    public void registerTenant(String nombreDb) {
        tenantRegistry.register(nombreDb);
    }
}

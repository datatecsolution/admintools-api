package net.datatecsolution.admintools.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * US-101 — Fachada para el alta en caliente de tenants (cajas) y para exponer
 * los datos de conexion por-caja al provisioner (Flyway necesita url/user/pass
 * de la BD recien creada). Instanciado como bean en {@link MultiTenantConfig},
 * que es quien conoce las propiedades tenant.datasource.*.
 */
public class TenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantRegistry.class);

    private final TenantRoutingDataSource routing;
    private final Function<String, HikariDataSource> dataSourceFactory;
    private final String urlPrefix;
    private final String urlSuffix;
    private final String username;
    private final String password;

    public TenantRegistry(TenantRoutingDataSource routing,
                          Function<String, HikariDataSource> dataSourceFactory,
                          String urlPrefix, String urlSuffix,
                          String username, String password) {
        this.routing = routing;
        this.dataSourceFactory = dataSourceFactory;
        this.urlPrefix = urlPrefix;
        this.urlSuffix = urlSuffix;
        this.username = username;
        this.password = password;
    }

    /** Registra el pool de una caja nueva; idempotente. */
    public void register(String nombreDb) {
        if (routing.hasTenant(nombreDb)) {
            return;
        }
        routing.registerTenant(nombreDb, dataSourceFactory.apply(nombreDb));
        log.info("Tenant DataSource registrado en caliente: {}", nombreDb);
    }

    public String jdbcUrlFor(String nombreDb) {
        return urlPrefix + nombreDb + urlSuffix;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }
}

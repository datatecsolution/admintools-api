package net.datatecsolution.admintools.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * US-017 — Multi-tenant DataSource config.
 *
 * Dos arboles de conexion:
 *
 *  commonDataSource (@Primary)
 *      → admin_tools   — entities globales (Cliente, Articulo, Bodega,
 *                        Existencia, Compras, etc.). Las JpaRepositories
 *                        existentes usan este por default.
 *
 *  tenantRoutingDataSource
 *      → admin_tools_caja_N — AbstractRoutingDataSource resuelve la caja
 *                              via TenantContext (poblado por TenantInterceptor).
 *                              Hoy usado por JdbcTemplate del smoke test;
 *                              INV-8 le agrega su propio EMF/TM con
 *                              entities Factura/DetalleFactura.
 *
 * Cuando se declaran DataSources manualmente, Spring Boot deja de
 * autoconfigurar JPA — hay que declarar tambien EntityManagerFactory y
 * TransactionManager. Lo hacemos aqui con @Primary para que toda la JPA
 * actual siga apuntando al commonDS sin cambios.
 *
 * Las cajas se descubren al boot leyendo {@code admin_tools.cajas.nombre_db}.
 * Reinicio del API requerido si se agrega caja en BD (mejora futura: refresh).
 */
@Configuration
public class MultiTenantConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantConfig.class);

    @Value("${tenant.datasource.url-prefix}")
    private String tenantUrlPrefix;

    @Value("${tenant.datasource.url-suffix}")
    private String tenantUrlSuffix;

    @Value("${tenant.datasource.username}")
    private String tenantUsername;

    @Value("${tenant.datasource.password}")
    private String tenantPassword;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String hibernateDdlAuto;

    @Value("${spring.datasource.url}")
    private String commonUrl;

    @Value("${spring.datasource.username}")
    private String commonUsername;

    @Value("${spring.datasource.password}")
    private String commonPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String commonDriver;

    // ============= Common DataSource (admin_tools) =============

    @Bean
    @Primary
    public DataSource commonDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(commonUrl);
        ds.setUsername(commonUsername);
        ds.setPassword(commonPassword);
        ds.setDriverClassName(commonDriver);
        ds.setPoolName("hikari-common");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        return ds;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("commonDataSource") DataSource ds,
            JpaProperties jpaProperties) {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("net.datatecsolution.admintools.persistence.entity");
        emf.setPersistenceUnitName("common");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>(jpaProperties.getProperties());
        // ddl-auto se setea como spring.jpa.hibernate.ddl-auto (raiz, no nested);
        // JpaProperties.getProperties() solo trae spring.jpa.properties.*
        props.put("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    // ============= Tenant Routing DataSource (admin_tools_caja_N) =============

    @Bean
    public DataSource tenantRoutingDataSource(@Qualifier("commonDataSource") DataSource commonDS) {
        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();

        try (var conn = commonDS.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT codigo, nombre_db FROM cajas")) {
            while (rs.next()) {
                int codigo = rs.getInt("codigo");
                String nombreDb = rs.getString("nombre_db");
                if (nombreDb == null || nombreDb.isBlank()) {
                    log.warn("Caja codigo={} tiene nombre_db vacio; se omite", codigo);
                    continue;
                }
                HikariDataSource cajaDs = buildCajaDataSource(nombreDb);
                targets.put(nombreDb, cajaDs);
                log.info("Tenant DataSource registrado: caja codigo={} → {}", codigo, nombreDb);
            }
        } catch (Exception e) {
            log.error("No se pudo descubrir las cajas desde admin_tools.cajas — multi-tenant no funcionara", e);
            throw new IllegalStateException("Multi-tenant init fallo: " + e.getMessage(), e);
        }

        if (targets.isEmpty()) {
            log.warn("admin_tools.cajas esta vacio — no hay tenants. Multi-tenant operativo pero sin destinos.");
        }

        routing.setTargetDataSources(targets);
        // Fallback cuando TenantContext esta vacio: caemos al commonDS para no
        // explotar. La logica de "este endpoint requiere caja" la enforce el
        // service de facturacion (INV-8).
        routing.setDefaultTargetDataSource(commonDS);
        routing.afterPropertiesSet();
        return routing;
    }

    private HikariDataSource buildCajaDataSource(String nombreDb) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(tenantUrlPrefix + nombreDb + tenantUrlSuffix);
        ds.setUsername(tenantUsername);
        ds.setPassword(tenantPassword);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setPoolName("hikari-" + nombreDb);
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        return ds;
    }
}

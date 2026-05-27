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
 * US-017 + INV-8 — DataSources y EntityManagerFactories multi-tenant.
 *
 * Dos arboles de persistencia:
 *
 *  commonDataSource (@Primary) → admin_tools
 *      JPA repos en {@code persistence.crud} → entities en {@code persistence.entity}
 *      (Cliente, Articulo, Bodega, Orden, ExistenciaArticuloBodega, etc.)
 *      Wiring de EMF/TM: {@link JpaConfigCommon}
 *
 *  tenantRoutingDataSource → admin_tools_caja_N
 *      AbstractRoutingDataSource que resuelve la caja via {@link TenantContext}.
 *      JPA repos en {@code persistence.tenant.crud} → entities en
 *      {@code persistence.tenant.entity} (EncabezadoFactura, DetalleFactura).
 *      Wiring de EMF/TM: {@link JpaConfigTenant}.
 *      Las cajas se descubren al boot leyendo admin_tools.cajas.nombre_db.
 *
 * Al declarar DataSources manualmente, Spring Boot deja de autoconfigurar JPA
 * — por eso las EMF/TM viven en las dos clases JpaConfig*, con esta clase solo
 * encargada de los pools.
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

    // ============= Tenant EntityManagerFactory + TransactionManager =============
    // Solo escanea persistence.tenant.entity. Las repos que lo usen viven en
    // persistence.tenant.crud (ver JpaConfigTenant). El DataSource detras es el
    // routing — la BD real (caja_1, caja_2…) se elige por request via TenantContext.

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            @Qualifier("tenantRoutingDataSource") DataSource tenantDs,
            JpaProperties jpaProperties) {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(tenantDs);
        emf.setPackagesToScan("net.datatecsolution.admintools.persistence.tenant.entity");
        emf.setPersistenceUnitName("tenant");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> props = new HashMap<>(jpaProperties.getProperties());
        // tenant siempre con ddl-auto=none: NO queremos que Hibernate intente
        // validar/crear schema contra una caja, son N BDs y el schema lo
        // maneja Flyway via Swing (V8 y baselines).
        props.put("hibernate.hbm2ddl.auto", "none");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}

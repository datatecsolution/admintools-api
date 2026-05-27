package net.datatecsolution.admintools.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityManagerFactory + TransactionManager + JPA Repositories del arbol COMMON
 * (admin_tools). Es el {@code @Primary} — todas las @Autowired sin qualifier
 * caen aqui (CrudRepository, EntityManager).
 *
 * Escanea solo {@code persistence.crud} y {@code persistence.entity} —
 * {@code persistence.tenant.*} esta cubierto por {@link JpaConfigTenant}.
 * Como tenant es un package PARALELO (no sub-package), no hay overlap.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "net.datatecsolution.admintools.persistence.crud",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class JpaConfigCommon {

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String hibernateDdlAuto;

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
}

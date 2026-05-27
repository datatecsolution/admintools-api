package net.datatecsolution.admintools.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Activa las JPA repositories del arbol TENANT (admin_tools_caja_N).
 *
 * Las dos beans tenantEntityManagerFactory + tenantTransactionManager las
 * declara {@link MultiTenantConfig}. Aqui solo se conectan a las repositories
 * del sub-arbol {@code persistence.tenant.crud}.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "net.datatecsolution.admintools.persistence.tenant.crud",
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class JpaConfigTenant {
}

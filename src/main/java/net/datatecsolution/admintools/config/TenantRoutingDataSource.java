package net.datatecsolution.admintools.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * DataSource dinamico para multi-tenant (US-017). Spring delega aqui cada
 * vez que necesita una conexion para entities del tenant; devolvemos la
 * conexion al DataSource de la caja activa segun {@link TenantContext}.
 *
 * Si TenantContext esta vacio (usuario sin caja, o request sin auth),
 * devuelve null → AbstractRoutingDataSource cae al defaultTargetDataSource.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }
}

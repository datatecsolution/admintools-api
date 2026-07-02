package net.datatecsolution.admintools.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataSource dinamico para multi-tenant (US-017). Spring delega aqui cada
 * vez que necesita una conexion para entities del tenant; devolvemos la
 * conexion al DataSource de la caja activa segun {@link TenantContext}.
 *
 * Si TenantContext esta vacio (usuario sin caja, o request sin auth),
 * devuelve null → AbstractRoutingDataSource cae al defaultTargetDataSource.
 *
 * US-101: mantiene su propio mapa de targets para poder registrar la BD de
 * una caja recien provisionada (POST /cajas) sin reiniciar la API.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targets = new ConcurrentHashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }

    /** Carga inicial de targets al boot (MultiTenantConfig). */
    public void setInitialTargets(Map<Object, Object> initial) {
        targets.putAll(initial);
        super.setTargetDataSources(targets);
    }

    /**
     * Alta en caliente del DataSource de una caja nueva. afterPropertiesSet()
     * re-resuelve el mapa interno (resolvedDataSources) que consulta cada request.
     */
    public synchronized void registerTenant(String nombreDb, DataSource ds) {
        targets.put(nombreDb, ds);
        super.setTargetDataSources(targets);
        super.afterPropertiesSet();
    }

    public boolean hasTenant(String nombreDb) {
        return targets.containsKey(nombreDb);
    }
}

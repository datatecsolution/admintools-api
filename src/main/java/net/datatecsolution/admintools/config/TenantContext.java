package net.datatecsolution.admintools.config;

/**
 * Holder per-request del tenant (= caja) activo. Lo setea
 * {@link TenantInterceptor} despues de la auth JWT y lo lee
 * {@link TenantRoutingDataSource} cuando JPA pide una conexion.
 *
 * Implementado con ThreadLocal — limpiar siempre en afterCompletion
 * del interceptor para evitar leaks entre requests reutilizando threads.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    /** Setea la BD de la caja activa para este request (ej. "admin_tools_caja_2"). */
    public static void setTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    /** Devuelve la BD de la caja activa, o {@code null} si no hay tenant (admin/tecnico sin caja). */
    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    /** Limpia el ThreadLocal. SIEMPRE llamar en afterCompletion. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

package net.datatecsolution.admintools.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource;
import java.security.Principal;

/**
 * Interceptor que resuelve la caja del usuario autenticado y la guarda
 * en {@link TenantContext} para que el {@link TenantRoutingDataSource}
 * la use cuando JPA pida una conexion.
 *
 * Corre DESPUES de {@code JwtRequestFilter} (que es un filtro de Spring
 * Security, ejecuta antes que cualquier interceptor MVC), asi que el
 * Principal ya esta seteado cuando llegamos aqui.
 *
 * Limpia el ThreadLocal en afterCompletion — critico para evitar leaks
 * entre requests reutilizando threads del pool de Tomcat.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    private final DataSource commonDataSource;

    @Autowired
    public TenantInterceptor(@Qualifier("commonDataSource") DataSource commonDataSource) {
        this.commonDataSource = commonDataSource;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // request sin auth (ej. /auth/login, /v3/api-docs) — sin tenant
            return true;
        }

        String username = auth.getName();
        String tenant = resolveTenantForUser(username);
        if (tenant != null) {
            TenantContext.setTenant(tenant);
            log.trace("Tenant resuelto para {}: {}", username, tenant);
        }
        // si tenant es null, el usuario no tiene caja asignada. NO bloqueamos
        // aqui — el endpoint que requiera caja (INV-8) chequea explicitamente
        // y devuelve 403 si hace falta. Endpoints que no la necesitan
        // (clientes, productos, etc.) funcionan igual.
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        TenantContext.clear();
    }

    /**
     * usuario.codigo_caja → cajas.nombre_db. Cero al login (admin/tecnico)
     * devuelve null = sin caja.
     */
    private String resolveTenantForUser(String username) {
        String sql = """
                SELECT c.nombre_db
                  FROM usuario u
                  JOIN cajas c ON c.codigo = u.codigo_caja
                 WHERE u.usuario = ? AND u.codigo_caja > 0
                 LIMIT 1
                """;
        try (var conn = commonDataSource.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            log.warn("Error resolviendo caja para usuario {}: {}", username, e.getMessage());
        }
        return null;
    }
}

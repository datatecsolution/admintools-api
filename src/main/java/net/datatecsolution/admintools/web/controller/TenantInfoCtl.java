package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.config.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smoke test del routing multi-tenant (US-017). NO es funcionalidad de
 * negocio — solo valida que TenantContext + TenantRoutingDataSource
 * estan funcionando correctamente.
 *
 * GET /tenant-info devuelve la caja resuelta para el usuario logueado y
 * cuenta cuantas facturas hay en SU caja. Si el usuario no tiene caja
 * asignada (admin/tecnico con codigo_caja=0), devuelve 403.
 *
 * Cuando hagamos INV-8, este controller puede borrarse — su unica
 * razon es validar el wiring de US-017.
 */
@RestController
@RequestMapping("/tenant-info")
@Tag(name = "Tenant Info", description = "Smoke test de routing multi-tenant (US-017)")
public class TenantInfoCtl {

    private final JdbcTemplate tenantJdbc;

    public TenantInfoCtl(@Qualifier("tenantRoutingDataSource") DataSource tenantDs) {
        this.tenantJdbc = new JdbcTemplate(tenantDs);
    }

    @GetMapping
    @Operation(summary = "Smoke test: devuelve caja del usuario y cuenta facturas en esa caja")
    public ResponseEntity<Map<String, Object>> info() {
        String tenant = TenantContext.getTenant();
        if (tenant == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuario sin caja asignada — no se puede acceder al tenant");
        }
        Integer count = tenantJdbc.queryForObject(
                "SELECT COUNT(*) FROM encabezado_factura", Integer.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant", tenant);
        body.put("facturasEnEstaCaja", count);
        return ResponseEntity.ok(body);
    }
}

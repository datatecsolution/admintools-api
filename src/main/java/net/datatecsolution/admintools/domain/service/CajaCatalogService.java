package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5 fix — catalogo de cajas (read-only).
 */
@Service
public class CajaCatalogService {

    private final CajaCRUD crud;

    public CajaCatalogService(CajaCRUD crud) {
        this.crud = crud;
    }

    public List<CajaResponse> getAll() {
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .sorted((a, b) -> a.getCodigo().compareTo(b.getCodigo()))
                .map(c -> new CajaResponse(c.getCodigo(), c.getDescripcion(),
                        c.getCodigoBodega(), c.getNombreDb()))
                .collect(Collectors.toList());
    }

    /**
     * Caja de la sesión actual: se resuelve por el tenant del JWT
     * (TenantContext guarda el nombre_db de la caja). Para mostrar la caja
     * activa en el POS sin exponer /users (ADMIN).
     */
    public Optional<CajaResponse> getCurrent() {
        String tenant = TenantContext.getTenant();
        if (tenant == null || tenant.isBlank()) return Optional.empty();
        return crud.findByNombreDb(tenant)
                .map(c -> new CajaResponse(c.getCodigo(), c.getDescripcion(),
                        c.getCodigoBodega(), c.getNombreDb()));
    }
}

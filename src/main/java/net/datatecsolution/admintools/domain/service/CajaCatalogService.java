package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5 fix — catalogo de cajas (read-only).
 * US-101: enriquece con el nombre de la bodega (espejo del JOIN de la
 * lista de cajas del Swing).
 */
@Service
public class CajaCatalogService {

    private final CajaCRUD crud;
    private final BodegaCRUD bodegaCRUD;

    public CajaCatalogService(CajaCRUD crud, BodegaCRUD bodegaCRUD) {
        this.crud = crud;
        this.bodegaCRUD = bodegaCRUD;
    }

    public List<CajaResponse> getAll() {
        Map<Integer, String> bodegas = bodegaNames();
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .sorted((a, b) -> a.getCodigo().compareTo(b.getCodigo()))
                .map(c -> toResponse(c, bodegas))
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
                .map(c -> toResponse(c, bodegaNames()));
    }

    private Map<Integer, String> bodegaNames() {
        return bodegaCRUD.findAll().stream()
                .collect(Collectors.toMap(Bodega::getCodigoBodega, Bodega::getDescripcionBodega));
    }

    private CajaResponse toResponse(Caja c, Map<Integer, String> bodegas) {
        return new CajaResponse(c.getCodigo(), c.getDescripcion(), c.getCodigoBodega(),
                bodegas.get(c.getCodigoBodega()), c.getNombreDb());
    }
}

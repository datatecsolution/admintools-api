package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import org.springframework.stereotype.Service;

import java.util.List;
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
}

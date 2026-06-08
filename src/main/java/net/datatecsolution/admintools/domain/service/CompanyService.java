package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CompanyRequest;
import net.datatecsolution.admintools.domain.dto.CompanyResponse;
import net.datatecsolution.admintools.persistence.crud.DatosEmpresaCRUD;
import net.datatecsolution.admintools.persistence.entity.DatosEmpresa;
import org.springframework.stereotype.Service;

/**
 * US-031 — Datos de la empresa. {@code datos_empresa} es single-row: getCompany
 * devuelve la unica fila (o un response vacio si aun no existe) y updateCompany
 * hace upsert (actualiza la fila existente o crea la primera).
 */
@Service
public class CompanyService {

    private final DatosEmpresaCRUD crud;

    public CompanyService(DatosEmpresaCRUD crud) {
        this.crud = crud;
    }

    public CompanyResponse getCompany() {
        return crud.findAll().stream().findFirst()
                .map(CompanyService::toResponse)
                .orElse(new CompanyResponse(null, "", "", "", "", "", "", null));
    }

    public CompanyResponse updateCompany(CompanyRequest req) {
        DatosEmpresa e = crud.findAll().stream().findFirst().orElseGet(DatosEmpresa::new);
        // columnas NOT NULL sin default -> coalesce a "" para no violar el constraint
        e.setNombre(nz(req.nombre()));
        e.setRtn(nz(req.rtn()));
        e.setTelefono(nz(req.telefono()));
        e.setCorreo(nz(req.correo()));
        e.setPropietario(nz(req.propietario()));
        e.setDireccion(nz(req.direccion()));
        e.setLogoUrl(req.logoUrl()); // nullable
        return toResponse(crud.save(e));
    }

    private static CompanyResponse toResponse(DatosEmpresa e) {
        return new CompanyResponse(
                e.getId(), e.getNombre(), e.getRtn(), e.getTelefono(),
                e.getCorreo(), e.getPropietario(), e.getDireccion(), e.getLogoUrl());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

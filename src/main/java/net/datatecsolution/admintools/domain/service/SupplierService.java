package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.SupplierRequest;
import net.datatecsolution.admintools.domain.dto.SupplierResponse;
import net.datatecsolution.admintools.persistence.crud.ProveedorCRUD;
import net.datatecsolution.admintools.persistence.entity.Proveedor;
import net.datatecsolution.admintools.persistence.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

/**
 * CRUD del maestro de proveedores (US-096). Completa el {@link SupplierService}
 * read-only de INV-5 con alta/edición/baja, migrando CtlProveedor/ProveedorDao
 * del Swing (tabla {@code proveedor}, PK {@code codigo_proveedor}).
 *
 * El {@code saldo} es derivado: {@code f_saldo_proveedor(codigo_proveedor)}.
 * El DELETE es seguro: 409 si el proveedor está referenciado en compras, CxP o
 * recibos de pago (el Swing borraba físico sin guarda).
 */
@Service
public class SupplierService {

    @Autowired private ProveedorCRUD crud;
    @Autowired private SupplierMapper mapper;

    /** JdbcTemplate sobre la BD común (saldo derivado + chequeo de referencias). */
    private final JdbcTemplate jdbc;

    public SupplierService(@Qualifier("commonDataSource") DataSource commonDS) {
        this.jdbc = new JdbcTemplate(commonDS);
    }

    /** Listado plano para dropdowns (Compras/Facturación) — sin saldo. */
    public List<SupplierResponse> getAll() {
        return mapper.toResponses(crud.findAll());
    }

    public SupplierResponse getById(int id) {
        Proveedor p = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier " + id + " not found"));
        return withBalance(mapper.toResponse(p), id);
    }

    /** Búsqueda paginada para la pantalla de gestión, con saldo por proveedor. */
    public Page<SupplierResponse> search(String q, int page, int size) {
        PageRequest pageReq = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "nombreProveedor"));
        Page<Proveedor> result = (q == null || q.isBlank())
                ? crud.findAll(pageReq)
                : crud.search(q.trim(), pageReq);
        return result.map(p -> withBalance(mapper.toResponse(p), p.getCodigoProveedor()));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Proveedor saved = crud.save(mapper.toEntity(request));
        return withBalance(mapper.toResponse(saved), saved.getCodigoProveedor());
    }

    @Transactional
    public SupplierResponse update(int id, SupplierRequest request) {
        Proveedor entity = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier " + id + " not found"));
        mapper.updateEntity(request, entity);
        Proveedor saved = crud.save(entity);
        return withBalance(mapper.toResponse(saved), id);
    }

    @Transactional
    public void delete(int id) {
        if (!crud.existsById(id)) {
            throw new EntityNotFoundException("Supplier " + id + " not found");
        }
        if (enUso(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El proveedor está referenciado en compras o cuentas por pagar; "
                            + "no se puede eliminar.");
        }
        try {
            crud.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            // red de seguridad: alguna FK no anticipada → 409 en vez de 500.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El proveedor tiene referencias que impiden eliminarlo.");
        }
    }

    /** Rellena el saldo derivado (f_saldo_proveedor) en la respuesta. */
    private SupplierResponse withBalance(SupplierResponse r, int id) {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT ifnull(f_saldo_proveedor(?), 0)", BigDecimal.class, id);
        return new SupplierResponse(r.id(), r.name(), r.phone(), r.mobile(), r.address(), saldo);
    }

    /** ¿El proveedor está referenciado por compras, CxP o recibos de pago? */
    private boolean enUso(int id) {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM encabezado_factura_compra WHERE codigo_proveedor = ?) + "
                + "(SELECT COUNT(*) FROM cuentas_por_pagar        WHERE codigo_proveedor = ?) + "
                + "(SELECT COUNT(*) FROM recibo_pago_proveedores  WHERE codigo_proveedor = ?)";
        Integer refs = jdbc.queryForObject(sql, Integer.class, id, id, id);
        return refs != null && refs > 0;
    }
}

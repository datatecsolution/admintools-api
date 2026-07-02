package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.CajaRequest;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * US-101 — alta/edicion de cajas, replica del flujo del Swing
 * (CtlCaja + CajaDao.registrar):
 *
 *   1. INSERT en cajas (descripcion + bodega)
 *   2. nombre_db = admin_tools_caja_{codigo} → UPDATE
 *   3. CREATE DATABASE
 *   4. Flyway baseline de caja parametrizada (CajaProvisioner)
 *   5. alta en caliente del tenant (sin reiniciar la API)
 *
 * A diferencia del Swing (que deja la fila si el CREATE/migrate falla),
 * aqui se compensa: DROP de la BD recien creada + delete de la fila.
 *
 * Sin DELETE de cajas: el Swing tampoco lo expone (una caja con historial
 * de facturas no se borra).
 */
@Service
public class CajaAdminService {

    private static final Logger log = LoggerFactory.getLogger(CajaAdminService.class);

    /** Mismo prefijo que ModeloDaoBasic.dbNameCaja del Swing. */
    static final String DB_PREFIX = "admin_tools_caja_";

    private final CajaCRUD cajaCRUD;
    private final BodegaCRUD bodegaCRUD;
    private final CajaProvisioner provisioner;

    public CajaAdminService(CajaCRUD cajaCRUD, BodegaCRUD bodegaCRUD, CajaProvisioner provisioner) {
        this.cajaCRUD = cajaCRUD;
        this.bodegaCRUD = bodegaCRUD;
        this.provisioner = provisioner;
    }

    public CajaResponse create(CajaRequest request) {
        Bodega bodega = requireBodega(request.warehouseId());

        // 1-2. fila + nombre_db derivado del codigo generado (espejo CajaDao.registrar)
        Caja caja = new Caja();
        caja.setDescripcion(request.description().trim());
        caja.setCodigoBodega(bodega.getCodigoBodega());
        // nombre_db es NOT NULL; placeholder hasta conocer el codigo generado
        caja.setNombreDb("");
        caja = cajaCRUD.save(caja);
        String nombreDb = DB_PREFIX + caja.getCodigo();
        caja.setNombreDb(nombreDb);
        caja = cajaCRUD.save(caja);

        // 3-5. provisioning fisico, con compensacion si falla
        boolean dbCreated = false;
        try {
            provisioner.createDatabase(nombreDb);
            dbCreated = true;
            provisioner.migrate(nombreDb, bodega.getCodigoBodega());
            provisioner.registerTenant(nombreDb);
        } catch (Exception e) {
            log.error("Provisioning de {} fallo; se compensa (drop BD + delete fila)", nombreDb, e);
            if (dbCreated) {
                try {
                    provisioner.dropDatabase(nombreDb);
                } catch (Exception drop) {
                    log.error("No se pudo eliminar la BD {} durante la compensacion", nombreDb, drop);
                }
            }
            cajaCRUD.deleteById(caja.getCodigo());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo provisionar la base de datos de la caja: " + e.getMessage(), e);
        }

        return toResponse(caja, bodega);
    }

    public CajaResponse update(Integer id, CajaRequest request) {
        Caja caja = cajaCRUD.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Caja " + id + " no existe"));
        Bodega bodega = requireBodega(request.warehouseId());

        caja.setDescripcion(request.description().trim());
        caja.setCodigoBodega(bodega.getCodigoBodega());
        caja = cajaCRUD.save(caja);
        return toResponse(caja, bodega);
    }

    private Bodega requireBodega(Integer warehouseId) {
        return bodegaCRUD.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La bodega " + warehouseId + " no existe"));
    }

    private CajaResponse toResponse(Caja caja, Bodega bodega) {
        return new CajaResponse(caja.getCodigo(), caja.getDescripcion(),
                caja.getCodigoBodega(), bodega.getDescripcionBodega(), caja.getNombreDb());
    }
}

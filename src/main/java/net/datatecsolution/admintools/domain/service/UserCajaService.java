package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.UserCajaResponse;
import net.datatecsolution.admintools.domain.dto.UserCajaUpsertRequest;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4.5 fix — asignaciones caja ↔ usuario (tabla cajas_usuarios).
 *
 * Modelo:
 *   - Un usuario tiene N filas en cajas_usuarios (cajas habilitadas).
 *   - Exactamente UNA fila tiene por_defecto=1 (la que carga al abrir
 *     facturacion).
 *   - usuario.codigo_caja del Usuario entity se sincroniza con la caja
 *     default (TenantInterceptor de US-017 lo lee para resolver el
 *     tenant del JWT).
 *
 * El PUT modela "guardar cambios" del modal de usuario: lista
 * completa de cajas con un default. Implementacion: deleteAll + saveAll
 * (es la forma mas simple sin tener que hacer diff fino; el set tipico
 * es de 1-5 filas).
 */
@Service
public class UserCajaService {

    /** US-104: cajero — límite 1-2 cajas y rotación automática. */
    private static final int TIPO_CAJERO = 2;

    /** US-110: vendedor — máximo UNA caja (el pedido hereda su caja/bodega, US-109). */
    private static final int TIPO_VENDEDOR = 3;

    private final CajaUsuarioCRUD crud;
    private final UsuarioCRUD usuariosCrud;
    private final CajaCRUD cajasCrud;
    private final ConfigUserFacturacionCRUD configCrud;

    public UserCajaService(CajaUsuarioCRUD crud, UsuarioCRUD usuariosCrud, CajaCRUD cajasCrud,
                           ConfigUserFacturacionCRUD configCrud) {
        this.crud = crud;
        this.usuariosCrud = usuariosCrud;
        this.cajasCrud = cajasCrud;
        this.configCrud = configCrud;
    }

    public List<UserCajaResponse> getByUser(int userId) {
        return getByUsername(loadUser(userId).getNombreUsuario());
    }

    /**
     * US-105 — cajas asignadas por USERNAME (para {@code GET /cajas/mine}:
     * el POS no conoce su id numérico, solo el principal del JWT).
     */
    public List<UserCajaResponse> getByUsername(String username) {
        Map<Integer, Caja> cajas = loadCajas();
        return crud.findByIdUsuario(username).stream()
                .sorted((a, b) -> a.getCodigoCaja().compareTo(b.getCodigoCaja()))
                .map(cu -> {
                    Caja c = cajas.get(cu.getCodigoCaja());
                    return new UserCajaResponse(
                            cu.getCodigoCaja(),
                            c != null ? c.getDescripcion() : "?",
                            cu.isPorDefecto(),
                            c != null ? c.getCodigoBodega() : null);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UserCajaResponse> replaceAll(int userId, List<UserCajaUpsertRequest> incoming) {
        Usuario u = loadUser(userId);
        Map<Integer, String> names = loadCajaNames();

        // 1) Validaciones del payload.
        // US-104: un cajero opera con 1 o 2 cajas (2 = habilita rotación
        // automática); otros tipos sin límite (admin general puede tener 0).
        boolean esCajero = u.getTipoPermiso() != null && u.getTipoPermiso() == TIPO_CAJERO;
        if (esCajero && (incoming.size() < 1 || incoming.size() > 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un cajero debe tener entre 1 y 2 cajas asignadas (recibidas: " + incoming.size() + ")");
        }

        // US-110: el pedido del vendedor hereda SU caja (US-109) — con varias,
        // la atribución de la reserva de stock sería ambigua. Máximo una.
        boolean esVendedor = u.getTipoPermiso() != null && u.getTipoPermiso() == TIPO_VENDEDOR;
        if (esVendedor && incoming.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un vendedor solo puede tener UNA caja asignada (recibidas: " + incoming.size() + ")");
        }

        if (incoming.isEmpty()) {
            // permitido: usuario sin cajas (admin general). Sync codigo_caja a 0.
            crud.deleteByIdUsuario(u.getNombreUsuario());
            u.setCodigoCaja(0);
            usuariosCrud.save(u);
            // US-104: sin cajas no hay rotación posible — se apaga el flag.
            configCrud.updateRotacionAutomaticaCajas(u.getNombreUsuario(), 0);
            return List.of();
        }

        // No duplicar cajaId.
        if (incoming.stream().map(UserCajaUpsertRequest::cajaId).distinct().count() != incoming.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cajaId duplicado en el request");
        }

        // Validar que todas existen.
        for (UserCajaUpsertRequest row : incoming) {
            if (!names.containsKey(row.cajaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "cajaId " + row.cajaId() + " no existe en el catalogo");
            }
        }

        // Exactamente un default.
        long defaults = incoming.stream().filter(UserCajaUpsertRequest::isDefault).count();
        if (defaults != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe haber exactamente 1 caja marcada como default (recibidas: " + defaults + ")");
        }

        // 2) Replace: borrar todo y volver a insertar (el set tipico es < 10 filas).
        crud.deleteByIdUsuario(u.getNombreUsuario());
        crud.flush();

        List<CajaUsuario> toSave = incoming.stream()
                .map(r -> new CajaUsuario(r.cajaId(), u.getNombreUsuario(), r.isDefault()))
                .collect(Collectors.toList());
        crud.saveAll(toSave);

        // 3) Sync usuario.codigo_caja con la caja default (para TenantInterceptor).
        Integer defaultCajaId = incoming.stream()
                .filter(UserCajaUpsertRequest::isDefault)
                .findFirst()
                .map(UserCajaUpsertRequest::cajaId)
                .orElseThrow();
        u.setCodigoCaja(defaultCajaId);
        usuariosCrud.save(u);

        // 4) US-104: la rotación automática exige EXACTAMENTE 2 cajas — si el
        //    set nuevo no las tiene, el flag se apaga (auto-apagado; mismo
        //    @Transactional, así que cajas y flag quedan coherentes o rollback).
        if (incoming.size() != 2) {
            configCrud.updateRotacionAutomaticaCajas(u.getNombreUsuario(), 0);
        }

        return getByUser(userId);
    }

    // ----- helpers -----

    private Usuario loadUser(int userId) {
        return usuariosCrud.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + userId + " no encontrado"));
    }

    private Map<Integer, String> loadCajaNames() {
        Map<Integer, String> m = new HashMap<>();
        StreamSupport.stream(cajasCrud.findAll().spliterator(), false)
                .forEach(c -> m.put(c.getCodigo(), c.getDescripcion()));
        return m;
    }

    /** US-105 pulido: entidades completas (nombre + bodega) para /mine. */
    private Map<Integer, Caja> loadCajas() {
        Map<Integer, Caja> m = new HashMap<>();
        StreamSupport.stream(cajasCrud.findAll().spliterator(), false)
                .forEach(c -> m.put(c.getCodigo(), c));
        return m;
    }
}

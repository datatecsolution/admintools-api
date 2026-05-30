package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.UserCajaResponse;
import net.datatecsolution.admintools.domain.dto.UserCajaUpsertRequest;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
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

    private final CajaUsuarioCRUD crud;
    private final UsuarioCRUD usuariosCrud;
    private final CajaCRUD cajasCrud;

    public UserCajaService(CajaUsuarioCRUD crud, UsuarioCRUD usuariosCrud, CajaCRUD cajasCrud) {
        this.crud = crud;
        this.usuariosCrud = usuariosCrud;
        this.cajasCrud = cajasCrud;
    }

    public List<UserCajaResponse> getByUser(int userId) {
        Usuario u = loadUser(userId);
        Map<Integer, String> names = loadCajaNames();
        return crud.findByIdUsuario(u.getNombreUsuario()).stream()
                .sorted((a, b) -> a.getCodigoCaja().compareTo(b.getCodigoCaja()))
                .map(cu -> new UserCajaResponse(
                        cu.getCodigoCaja(),
                        names.getOrDefault(cu.getCodigoCaja(), "?"),
                        cu.isPorDefecto()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UserCajaResponse> replaceAll(int userId, List<UserCajaUpsertRequest> incoming) {
        Usuario u = loadUser(userId);
        Map<Integer, String> names = loadCajaNames();

        // 1) Validaciones del payload.
        if (incoming.isEmpty()) {
            // permitido: usuario sin cajas (admin general). Sync codigo_caja a 0.
            crud.deleteByIdUsuario(u.getNombreUsuario());
            u.setCodigoCaja(0);
            usuariosCrud.save(u);
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
}

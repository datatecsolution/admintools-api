package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PasswordResetRequest;
import net.datatecsolution.admintools.domain.dto.UserCreateRequest;
import net.datatecsolution.admintools.domain.dto.UserResponse;
import net.datatecsolution.admintools.domain.dto.UserUpdateRequest;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Sprint 4 #51 — administracion de usuarios del sistema.
 *
 * Separado del legacy {@code UserService} (que solo expone getAll para
 * compatibilidad con codigo Sprint 1/2) para no contaminarlo con la
 * logica de admin (BCrypt, validaciones, soft delete).
 *
 * Decisiones:
 *   - Password se hashea con BCrypt (PasswordEncoder bean de SecurityConfig).
 *   - Soft delete: DELETE setea enabled=false, NO elimina la fila.
 *   - Reset de password en endpoint separado para evitar que PUT lo toque
 *     accidentalmente.
 *   - Username no es editable post-creacion (Spring Security lo usa como
 *     identificador y romperia sesiones activas).
 */
@Service
public class UserAdminService {

    /** US-104: cajero (mapRole → CASHIER); único tipo con rotación de cajas. */
    private static final int TIPO_CAJERO = 2;

    private final UsuarioCRUD crud;
    private final PasswordEncoder passwordEncoder;
    private final CajaUsuarioCRUD cajaUsuarioCrud;
    private final ConfigUserFacturacionCRUD configCrud;

    public UserAdminService(UsuarioCRUD crud, PasswordEncoder passwordEncoder,
                            CajaUsuarioCRUD cajaUsuarioCrud,
                            ConfigUserFacturacionCRUD configCrud) {
        this.crud = crud;
        this.passwordEncoder = passwordEncoder;
        this.cajaUsuarioCrud = cajaUsuarioCrud;
        this.configCrud = configCrud;
    }

    public List<UserResponse> getAll() {
        // US-104: flags de rotación en bulk (1 query) para evitar N+1.
        Map<String, Boolean> flags = new HashMap<>();
        for (Object[] row : configCrud.findAllRotacionFlags()) {
            flags.put((String) row[0], ((Number) row[1]).intValue() == 1);
        }
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .map(u -> toResponse(u, flags.getOrDefault(u.getNombreUsuario(), false)))
                .collect(Collectors.toList());
    }

    public UserResponse getById(int id) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        return toResponse(u, loadRotacion(u));
    }

    public UserResponse create(UserCreateRequest req) {
        if (crud.findByNombreUsuario(req.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username '" + req.username() + "' ya existe");
        }
        Usuario u = new Usuario();
        u.setNombreUsuario(req.username());
        u.setContraseniaUsuario(passwordEncoder.encode(req.password()));
        u.setNombreCompleto(req.nombreCompleto() != null ? req.nombreCompleto() : "NA");
        u.setTipoPermiso(req.tipoPermiso());
        u.setCodigoCaja(req.codigoCaja() != null ? req.codigoCaja() : 0);
        u.setCodigoEmpleado(req.codigoEmpleado());
        u.setEnabled(true);
        // El trigger usuario_a_insert crea la fila de config con flag=0.
        return toResponse(crud.save(u), false);
    }

    @Transactional
    public UserResponse update(int id, UserUpdateRequest req) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        if (req.nombreCompleto() != null) u.setNombreCompleto(req.nombreCompleto());
        u.setTipoPermiso(req.tipoPermiso());
        if (req.codigoCaja() != null) u.setCodigoCaja(req.codigoCaja());
        if (req.codigoEmpleado() != null) u.setCodigoEmpleado(req.codigoEmpleado());
        Usuario saved = crud.save(u);
        // US-104: la rotación es exclusiva de cajeros — si el tipo deja de ser
        // cajero, se apaga SIEMPRE (idempotente; normaliza también estados
        // inconsistentes que pueda dejar el Swing).
        if (req.tipoPermiso() == null || req.tipoPermiso() != TIPO_CAJERO) {
            configCrud.updateRotacionAutomaticaCajas(saved.getNombreUsuario(), 0);
        }
        return toResponse(saved, loadRotacion(saved));
    }

    /**
     * US-104 — enciende/apaga la rotación automática de cajas del usuario.
     * Encender exige cajero con exactamente 2 cajas asignadas (la precondición
     * que asume RotacionCajasService); apagar es incondicional.
     */
    @Transactional
    public void setRotacion(int id, boolean enabled) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        if (!enabled) {
            // rowcount 0 (usuario legacy sin fila de config) = ya está apagado.
            configCrud.updateRotacionAutomaticaCajas(u.getNombreUsuario(), 0);
            return;
        }
        if (u.getTipoPermiso() == null || u.getTipoPermiso() != TIPO_CAJERO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solo un usuario cajero puede tener rotación automática de cajas");
        }
        int cajas = cajaUsuarioCrud.findByIdUsuario(u.getNombreUsuario()).size();
        if (cajas != 2) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La rotación automática requiere exactamente 2 cajas asignadas (tiene " + cajas + ")");
        }
        int rows = configCrud.updateRotacionAutomaticaCajas(u.getNombreUsuario(), 1);
        if (rows == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El usuario no tiene configuración de facturación (config_user_facturacion)");
        }
    }

    public void softDelete(int id) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        u.setEnabled(false);
        crud.save(u);
    }

    public void resetPassword(int id, PasswordResetRequest req) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        u.setContraseniaUsuario(passwordEncoder.encode(req.newPassword()));
        crud.save(u);
    }

    // ---- helpers ----

    private Boolean loadRotacion(Usuario u) {
        return configCrud.findRotacionAutomaticaCajas(u.getNombreUsuario())
                .map(v -> v == 1)
                .orElse(false);
    }

    private UserResponse toResponse(Usuario u, Boolean rotacion) {
        return new UserResponse(
                u.getIdUsuario() == null ? null : u.getIdUsuario().intValue(),
                u.getNombreUsuario(),
                u.getNombreCompleto(),
                u.getTipoPermiso(),
                mapRole(u.getTipoPermiso()),
                u.getCodigoCaja(),
                u.getCodigoEmpleado(),
                u.getEnabled(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                rotacion
        );
    }

    private String mapRole(Integer tipoPermiso) {
        if (tipoPermiso == null) return "USER";
        return switch (tipoPermiso) {
            case 4 -> "ADMIN";
            case 1 -> "INVENTORY";
            case 2 -> "CASHIER";
            case 3 -> "SELLER";
            default -> "USER";
        };
    }
}

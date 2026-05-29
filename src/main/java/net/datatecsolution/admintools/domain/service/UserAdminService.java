package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PasswordResetRequest;
import net.datatecsolution.admintools.domain.dto.UserCreateRequest;
import net.datatecsolution.admintools.domain.dto.UserResponse;
import net.datatecsolution.admintools.domain.dto.UserUpdateRequest;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

    private final UsuarioCRUD crud;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UsuarioCRUD crud, PasswordEncoder passwordEncoder) {
        this.crud = crud;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAll() {
        return StreamSupport.stream(crud.findAll().spliterator(), false)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getById(int id) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        return toResponse(u);
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
        return toResponse(crud.save(u));
    }

    public UserResponse update(int id, UserUpdateRequest req) {
        Usuario u = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario " + id + " no encontrado"));
        if (req.nombreCompleto() != null) u.setNombreCompleto(req.nombreCompleto());
        u.setTipoPermiso(req.tipoPermiso());
        if (req.codigoCaja() != null) u.setCodigoCaja(req.codigoCaja());
        if (req.codigoEmpleado() != null) u.setCodigoEmpleado(req.codigoEmpleado());
        return toResponse(crud.save(u));
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

    private UserResponse toResponse(Usuario u) {
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
                u.getUpdatedAt()
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

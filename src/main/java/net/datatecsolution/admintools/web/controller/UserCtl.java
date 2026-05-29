package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.datatecsolution.admintools.domain.dto.PasswordResetRequest;
import net.datatecsolution.admintools.domain.dto.UserCreateRequest;
import net.datatecsolution.admintools.domain.dto.UserResponse;
import net.datatecsolution.admintools.domain.dto.UserUpdateRequest;
import net.datatecsolution.admintools.domain.service.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 4 #51 — CRUD completo de usuarios del sistema.
 *
 * Backend de US-029 (frontend). RBAC ADMIN en todos los endpoints
 * (la gestion de usuarios es exclusiva del root). Patron US-019: DTOs
 * records + @Valid + GlobalExceptionHandler.
 *
 * Reemplaza el UserCtl vacio anterior. El metodo legacy
 * {@code GET /users/all} del UserService viejo NO se expone aqui (ya
 * estaba comentado en el original).
 */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "Administracion de usuarios del sistema (US-029 backend)")
public class UserCtl {

    private final UserAdminService service;

    public UserCtl(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios (sin password)")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id (sin password)")
    public ResponseEntity<UserResponse> getById(@PathVariable int id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un usuario nuevo (password se hashea con BCrypt)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario (sin tocar username ni password)")
    public ResponseEntity<UserResponse> update(@PathVariable int id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete: setea enabled=false sin borrar la fila")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/password")
    @Operation(summary = "Reset de password (se hashea con BCrypt)")
    public ResponseEntity<Void> resetPassword(@PathVariable int id,
                                              @Valid @RequestBody PasswordResetRequest request) {
        service.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}

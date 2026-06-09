package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Autorización por contraseña de administrador — réplica de
 * {@code UsuarioDao.comprobarAdmin(pwd)} del Swing: la contraseña ingresada se
 * compara contra TODOS los usuarios con {@code tipo_permiso} 1 (supervisor) o 4
 * (admin); si coincide con la de cualquiera, autoriza. Solo se necesita la
 * contraseña (sin usuario), igual que el Swing.
 *
 * Gatea las acciones protegidas de la facturación (Precios / Descuentos) según
 * las banderas {@code pwd_precio} / {@code pwd_descuento} de cada cajero.
 */
@Service
public class AdminAuthorizationService {

    /** tipo_permiso: 4 = root/ADMIN, 1 = supervisor/INVENTORY. */
    private static final List<Integer> ADMIN_PERMISOS = List.of(1, 4);

    private final UsuarioCRUD usuarioCRUD;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthorizationService(UsuarioCRUD usuarioCRUD, PasswordEncoder passwordEncoder) {
        this.usuarioCRUD = usuarioCRUD;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @return true si {@code rawPassword} coincide (BCrypt) con la de algún
     *         usuario admin/supervisor.
     */
    public boolean verifyAdminPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        for (Usuario u : usuarioCRUD.findByTipoPermisoIn(ADMIN_PERMISOS)) {
            String stored = u.getContraseniaUsuario();
            if (stored != null && !stored.isBlank() && passwordEncoder.matches(rawPassword, stored)) {
                return true;
            }
        }
        return false;
    }
}

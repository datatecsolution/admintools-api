package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.repository.UserRepository;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * Cargador de UserDetails para Spring Security.
 *
 * Las authorities se derivan de {@code usuario.tipo_permiso} — fuente unica
 * de verdad heredada del Swing:
 *   4 = root       -> ROLE_ADMIN
 *   1 = supervisor -> ROLE_INVENTORY
 *   2 = cajero     -> ROLE_CASHIER
 *   3 = vendedor   -> ROLE_SELLER
 *   otro/null      -> ROLE_USER (acceso minimo a endpoints autenticados)
 *
 * La jerarquia ADMIN > INVENTORY > CASHIER > SELLER > USER (definida en
 * SecurityConfig.roleHierarchy) hace que un solo rol implique todos los
 * menores, asi que basta asignar el mas alto.
 *
 * Devuelve un {@link User} de Spring Security (no la entidad Usuario) para
 * desacoplar la autoridad construida del fetch eager de relaciones.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> opt = userRepository.findByUsername(username);
        Usuario usuario = opt.orElseThrow(() ->
                new UsernameNotFoundException("Usuario no encontrado: " + username));

        GrantedAuthority authority = new SimpleGrantedAuthority(mapTipoPermiso(usuario.getTipoPermiso()));
        return new User(
                usuario.getNombreUsuario(),
                usuario.getContraseniaUsuario(),
                Collections.singletonList(authority));
    }

    private String mapTipoPermiso(Integer tipoPermiso) {
        if (tipoPermiso == null) return "ROLE_USER";
        return switch (tipoPermiso) {
            case 4 -> "ROLE_ADMIN";
            case 1 -> "ROLE_INVENTORY";
            case 2 -> "ROLE_CASHIER";
            case 3 -> "ROLE_SELLER";
            default -> "ROLE_USER";
        };
    }
}

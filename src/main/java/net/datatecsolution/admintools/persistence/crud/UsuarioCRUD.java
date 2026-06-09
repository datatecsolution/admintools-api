package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioCRUD extends CrudRepository<Usuario, Integer> {
    Optional<Usuario> findByNombreUsuario(String username);
    Usuario findUsuarioByNombreUsuario(String username);

    /** Usuarios por nivel de permiso (1=supervisor, 4=admin) — para comprobarAdmin. */
    List<Usuario> findByTipoPermisoIn(Collection<Integer> tiposPermiso);
}

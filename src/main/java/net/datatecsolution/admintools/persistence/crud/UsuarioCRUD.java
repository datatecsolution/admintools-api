package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UsuarioCRUD extends CrudRepository<Usuario, Integer> {
    Optional<Usuario> findByNombreUsuario(String username);
    Usuario findUsuarioByNombreUsuario(String username);
   // Usuario findByNombreUsuario(String username);
    //Usuario findByUsername(String username);
}

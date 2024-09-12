package net.datatecsolution.admintools.domain.repository;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository {

    // Método para encontrar un usuario por su nombre de usuario
    Optional<Usuario> findByUsername(String username);
}

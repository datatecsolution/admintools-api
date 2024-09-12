package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.repository.UserRepository;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import net.datatecsolution.admintools.persistence.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepository implements UserRepository {


    @Autowired
    private UsuarioCRUD usuarioCRUD;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Optional<Usuario> findByUsername(String username) {

        Optional<Usuario> usuarioOptional = usuarioCRUD.findByNombreUsuario(username);
        if (usuarioOptional.isPresent()) {
            return usuarioOptional;
        } else {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
    }

//    public Usuario findByNombreUsuario(String username) {
//
//        System.out.println("ID USUARIO =======>"+username);
//        Optional<Usuario> usuarioOptional = usuarioCRUD.findByNombreUsuario(username);
//
//        if(usuario==null) {
//            System.out.println("NOMBRE USUARIO ======>"+usuario);
//        }else{
//            System.out.println("NOMBRE USUARIO ======>"+usuario.getNombreUsuario());
//        }
//        return usuario;
//    }
//    @Override
//    public Optional<User> findByUsername(String username) {
//        Usuario usuario=usuarioCRUD.findByNombreUsuario(username);
//        return Optional.ofNullable(userMapper.toUser(usuario));
//    }
}

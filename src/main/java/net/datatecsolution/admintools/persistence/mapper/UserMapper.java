package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.User;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mappings({
            @Mapping(source = "idUsuario", target = "userId"),
            @Mapping(source = "nombreUsuario", target = "username"),
            @Mapping(source = "contraseniaUsuario", target = "password")
    })
    User toUser(Usuario usuario);

    @InheritInverseConfiguration
    Usuario toUsuario(User user);

    List<User> toUsers(List<Usuario> usuarios);
}

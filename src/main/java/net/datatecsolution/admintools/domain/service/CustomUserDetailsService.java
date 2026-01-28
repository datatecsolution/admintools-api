package net.datatecsolution.admintools.domain.service;
import net.datatecsolution.admintools.domain.User;
import net.datatecsolution.admintools.persistence.UsuarioRepository;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import net.datatecsolution.admintools.persistence.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import net.datatecsolution.admintools.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    public CustomUserDetailsService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;

    }

//
//    @Autowired
//    private UserRepository userRepository;  // Asume que tienes un repositorio para acceder a los usuarios

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        Optional<User> user = userRepository.findByUserDominio(username);
//
//        return userMapper.toUsuario(user.get());//orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));


        //se busca el usuario por el userName
        Optional<Usuario> user = userRepository.findByUsername(username);
        UserDetails userDetails= user.get();
        return userDetails;//.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

    }
}
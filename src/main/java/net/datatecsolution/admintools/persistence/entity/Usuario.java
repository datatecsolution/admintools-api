package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {


    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(name = "usuario",unique = true, nullable = false)
    private String nombreUsuario;

    @Column(name = "clave", nullable = false)
    private String contraseniaUsuario;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Set<Authority> authorities = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return contraseniaUsuario;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContraseniaUsuario() {
        return contraseniaUsuario;
    }

    public void setContraseniaUsuario(String contraseniaUsuario) {
        this.contraseniaUsuario = contraseniaUsuario;
    }


}

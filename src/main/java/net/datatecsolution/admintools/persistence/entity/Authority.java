package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "authorities")
public class Authority implements GrantedAuthority {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username")
    private Usuario usuario;

    @Column(name = "authority", nullable = false)
    private String authority;

    @Override
    public String getAuthority() {
        return authority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    // Getters y setters...
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Sprint 4.5 fix — PK compuesta de cajas_usuarios.
 *
 * La tabla NO tiene PK explicita en BD; la usamos a nivel JPA para
 * que Hibernate pueda manejar el entity (insert/update/delete por
 * combinacion).
 */
@Embeddable
public class CajaUsuarioKey implements Serializable {

    @Column(name = "codigo_caja")
    private Integer codigoCaja;

    @Column(name = "usuario")
    private String usuario;

    public CajaUsuarioKey() {}

    public CajaUsuarioKey(Integer codigoCaja, String usuario) {
        this.codigoCaja = codigoCaja;
        this.usuario = usuario;
    }

    public Integer getCodigoCaja() { return codigoCaja; }
    public void setCodigoCaja(Integer codigoCaja) { this.codigoCaja = codigoCaja; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CajaUsuarioKey k)) return false;
        return Objects.equals(codigoCaja, k.codigoCaja) && Objects.equals(usuario, k.usuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCaja, usuario);
    }
}

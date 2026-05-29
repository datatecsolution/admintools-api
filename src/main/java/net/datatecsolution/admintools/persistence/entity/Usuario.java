package net.datatecsolution.admintools.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {

    // La columna usuario.id es INT en BD; el campo Java es Long por
    // legacy. @JdbcTypeCode alinea el tipo JDBC esperado con INTEGER
    // sin tener que cambiar Long -> Integer (rompe consumidores).
    @Id
    @Column(name = "id")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(name = "usuario", unique = true, nullable = false)
    private String nombreUsuario;

    @Column(name = "clave", nullable = false)
    private String contraseniaUsuario;

    /**
     * Nivel de permiso del Swing legacy. Fuente unica de verdad de roles:
     *   4 = root       -> ROLE_ADMIN
     *   1 = supervisor -> ROLE_INVENTORY
     *   2 = cajero     -> ROLE_CASHIER
     *   3 = vendedor   -> ROLE_SELLER
     *   otro/null      -> ROLE_USER
     *
     * El mapeo se hace en CustomUserDetailsService; la tabla authorities
     * (que existe del setup inicial Spring Security) queda historica y NO
     * se consulta.
     */
    @Column(name = "tipo_permiso")
    private Integer tipoPermiso;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(name = "codigo_caja")
    private Integer codigoCaja;

    @Column(name = "codigo_empleado")
    private Integer codigoEmpleado;

    /**
     * Soft-delete flag (Sprint 4 #51). NULL o true = activo; false = baja
     * logica. Override del isEnabled() de UserDetails lee este campo.
     */
    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Authority> authorities = new HashSet<>();

    @OneToMany(mappedBy = "use")
    private List<UsuarioPrecio> precios;

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
        // Sprint 4 #51: lee el campo enabled de la BD (soft-delete).
        // NULL se considera activo (compatibilidad historica donde la
        // columna no estaba poblada).
        return enabled == null || enabled;
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

    public Integer getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(Integer tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public List<UsuarioPrecio> getPrecios() {
        return precios;
    }

    public void setPrecios(List<UsuarioPrecio> precios) {
        this.precios = precios;
    }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public Integer getCodigoCaja() { return codigoCaja; }
    public void setCodigoCaja(Integer codigoCaja) { this.codigoCaja = codigoCaja; }

    public Integer getCodigoEmpleado() { return codigoEmpleado; }
    public void setCodigoEmpleado(Integer codigoEmpleado) { this.codigoEmpleado = codigoEmpleado; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Sprint 4.5 fix — asignacion caja ↔ usuario.
 *
 * Modelo: un usuario tiene N filas en cajas_usuarios (una por caja a
 * la que puede entrar). por_defecto = 1 marca la caja que carga al
 * abrir facturacion. El TenantInterceptor de US-017 lee la columna
 * `usuario.codigo_caja` (no esta tabla); el sync con el default lo
 * hace el service al guardar.
 */
@Entity
@Table(name = "cajas_usuarios")
public class CajaUsuario {

    @EmbeddedId
    private CajaUsuarioKey id;

    @Column(name = "por_defecto", nullable = false)
    private Integer porDefecto;

    public CajaUsuario() {}

    public CajaUsuario(Integer codigoCaja, String usuario, boolean porDefecto) {
        this.id = new CajaUsuarioKey(codigoCaja, usuario);
        this.porDefecto = porDefecto ? 1 : 0;
    }

    public CajaUsuarioKey getId() { return id; }
    public void setId(CajaUsuarioKey id) { this.id = id; }

    public Integer getCodigoCaja() { return id != null ? id.getCodigoCaja() : null; }
    public String getUsuario() { return id != null ? id.getUsuario() : null; }

    public Integer getPorDefecto() { return porDefecto; }
    public void setPorDefecto(Integer porDefecto) { this.porDefecto = porDefecto; }

    public boolean isPorDefecto() { return porDefecto != null && porDefecto != 0; }
    public void setIsPorDefecto(boolean v) { this.porDefecto = v ? 1 : 0; }
}

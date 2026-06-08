package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Config de facturación por usuario (BD común). Por ahora solo se lee
 * {@code ventana_vendedor} (¿mostrar selector de vendedor al facturar?), vía
 * query nativa. Mapea el mínimo de columnas seguras para ddl-auto=validate.
 */
@Entity
@Table(name = "config_user_facturacion")
public class ConfigUserFacturacion {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario")
    private String usuario;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * US-031 — Datos generales de la empresa (BD comun, tabla single-row). Lo que
 * el negocio configura una vez (nombre, RTN, contacto, propietario, logo) y
 * que aparece en facturas y reportes. {@code logo_url} se agrega en V28.
 *
 * Los datos fiscales de facturacion (CAI/rangos, tabla datos_factura) ya NO
 * viven aqui: se movieron a las BD de caja (tenant) y son un modulo aparte.
 */
@Entity
@Table(name = "datos_empresa")
public class DatosEmpresa {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "rtn")
    private String rtn;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "correo")
    private String correo;

    @Column(name = "propietario")
    private String propietario;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "logo_url")
    private String logoUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRtn() {
        return rtn;
    }

    public void setRtn(String rtn) {
        this.rtn = rtn;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}

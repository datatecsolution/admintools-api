package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad para la tabla {@code proveedor}. INV-5 la usa read-only para
 * exponer {@code GET /suppliers} y para validar el FK al crear compras.
 * El CRUD completo de proveedores se difiere a una historia aparte.
 */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @Column(name = "codigo_proveedor")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoProveedor;

    @Column(name = "nombre_proveedor")
    private String nombreProveedor;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "celular")
    private String celular;

    @Column(name = "direccion")
    private String direccion;

    public Integer getCodigoProveedor() { return codigoProveedor; }
    public void setCodigoProveedor(Integer codigoProveedor) { this.codigoProveedor = codigoProveedor; }

    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}

package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad para 'departamento' — espejo de {@link Bodega}.
 * Los triggers de requisicion del kardex usan {@code codigo_depart_origen}
 * y {@code codigo_depart_destino} para resolver el kardex via codigo_bodega,
 * asi que ambos codigos DEBEN coincidir. {@code BodegaService} mantiene
 * el espejo de manera transaccional.
 *
 * No usamos @GeneratedValue: cuando creamos la Bodega usamos el codigo
 * calculado como max(MAX(bodega), MAX(departamento))+1 y lo asignamos
 * explicito a ambas tablas (patron de V17).
 */
@Entity
@Table(name = "departamento")
public class Departamento {

    @Id
    @Column(name = "codigo_departamento")
    private Integer codigoDepartamento;

    @Column(name = "nombre")
    private String nombre;

    public Departamento() {
    }

    public Departamento(Integer codigoDepartamento, String nombre) {
        this.codigoDepartamento = codigoDepartamento;
        this.nombre = nombre;
    }

    public Integer getCodigoDepartamento() { return codigoDepartamento; }
    public void setCodigoDepartamento(Integer codigoDepartamento) { this.codigoDepartamento = codigoDepartamento; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}

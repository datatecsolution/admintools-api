package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sprint 4.5 fix — caja del sistema.
 *
 * Mapea la tabla `cajas` (catalogo de cajas de cada cliente). Solo se
 * usa de read en el panel admin (POST/PUT de cajas se hace por flyway
 * o por la app Swing, no es operativo desde la web).
 */
@Entity
@Table(name = "cajas")
public class Caja {

    @Id
    @Column(name = "codigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "codigo_bodega")
    private Integer codigoBodega;

    @Column(name = "nombre_db")
    private String nombreDb;

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCodigoBodega() {
        return codigoBodega;
    }

    public void setCodigoBodega(Integer codigoBodega) {
        this.codigoBodega = codigoBodega;
    }

    public String getNombreDb() {
        return nombreDb;
    }

    public void setNombreDb(String nombreDb) {
        this.nombreDb = nombreDb;
    }
}

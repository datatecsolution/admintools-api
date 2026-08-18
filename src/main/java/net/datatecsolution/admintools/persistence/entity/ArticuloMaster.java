package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad ESCRIBIBLE para la tabla {@code articulo} (master data del producto).
 *
 * Coexiste con {@link Articulo}, que mapea a la vista @Immutable
 * {@code articulo_view} (lectura optimizada con existencia bundleada
 * bodega=1). Esta entidad es para las operaciones de write del CRUD de
 * producto (INV-4). El stock se lee aparte por INV-1 (tabla
 * {@code existencia_articulo_bodega}).
 */
@Entity
@Table(name = "articulo")
public class ArticuloMaster {

    @Id
    @Column(name = "codigo_articulo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoArticulo;

    @Column(name = "articulo", nullable = false)
    private String articulo;

    @Column(name = "codigo_marca", nullable = false)
    private Integer codigoMarca;

    @Column(name = "cod_articulo", nullable = false)
    private Integer codArticulo;

    @Column(name = "codigo_impuesto", nullable = false)
    private Integer codigoImpuesto;

    // precio_articulo migrado a DECIMAL(15,2) en V36 (US-072). Double +
    // @JdbcTypeCode(DECIMAL) para escribir contra la columna decimal sin
    // refactor a BigDecimal (Hibernate bindea BigDecimal.valueOf(double)).
    @Column(name = "precio_articulo", nullable = false)
    @JdbcTypeCode(SqlTypes.DECIMAL)
    private Double precioArticulo;

    @Column(name = "tipo_articulo", nullable = false)
    private Integer tipoArticulo;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    // US-146: 1 = el producto requiere pesarse en el POS (bascula); la
    // cantidad de venta es el peso en libras. Columna V46 (repo Swing).
    // Inicializado a FALSE porque la columna es NOT NULL y no todas las
    // altas pasan por el mapper (importer).
    @Column(name = "se_pesa", nullable = false)
    private Boolean sePesa = Boolean.FALSE;

    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }

    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }

    public Integer getCodigoMarca() { return codigoMarca; }
    public void setCodigoMarca(Integer codigoMarca) { this.codigoMarca = codigoMarca; }

    public Integer getCodArticulo() { return codArticulo; }
    public void setCodArticulo(Integer codArticulo) { this.codArticulo = codArticulo; }

    public Integer getCodigoImpuesto() { return codigoImpuesto; }
    public void setCodigoImpuesto(Integer codigoImpuesto) { this.codigoImpuesto = codigoImpuesto; }

    public Double getPrecioArticulo() { return precioArticulo; }
    public void setPrecioArticulo(Double precioArticulo) { this.precioArticulo = precioArticulo; }

    public Integer getTipoArticulo() { return tipoArticulo; }
    public void setTipoArticulo(Integer tipoArticulo) { this.tipoArticulo = tipoArticulo; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public Boolean getSePesa() { return sePesa; }
    public void setSePesa(Boolean sePesa) { this.sePesa = sePesa; }
}

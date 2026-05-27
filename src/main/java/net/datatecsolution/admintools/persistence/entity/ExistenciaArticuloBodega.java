package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de la tabla materializada {@code existencia_articulo_bodega}
 * (creada por la migracion V18 del repo Swing). Cache transaccional del
 * saldo actual del kardex por (articulo, bodega).
 *
 * La tabla la mantienen los SPs {@code crear_*_kardex} (V19, V20) en la
 * misma transaccion que el kardex; aqui solo la leemos. Habilita lecturas
 * de stock O(1) multi-bodega, vs la funcion {@code f_can_saldo_kardex}
 * que escanea kardex cada vez.
 *
 * Introducida en INV-1. Ver docs/inventario-api-design.md §3 D2.
 */
@Entity
@Table(name = "existencia_articulo_bodega")
@IdClass(ExistenciaArticuloBodegaId.class)
public class ExistenciaArticuloBodega {

    @Id
    @Column(name = "codigo_articulo")
    private Integer codigoArticulo;

    @Id
    @Column(name = "codigo_bodega")
    private Integer codigoBodega;

    @Column(name = "cantidad")
    private BigDecimal cantidad;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    // Para enriquecer con el nombre de la bodega sin un JOIN explicito.
    // insertable/updatable=false porque codigo_bodega ya esta mapeado arriba como @Id.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "codigo_bodega", insertable = false, updatable = false)
    private Bodega bodega;

    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }

    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }

    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}

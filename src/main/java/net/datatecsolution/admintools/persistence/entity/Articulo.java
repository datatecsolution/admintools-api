package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "articulo_view")
@Immutable
public class Articulo {
    @Id
    @Column(name = "codigo_articulo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer articuloId;

    @Column(name = "articulo")
    private String articulo;

    @Column(name = "precio_articulo")
    private Double precioVenta;

    @Column(name = "estado")
    private Boolean estado;

    @Column(name = "tipo_articulo")
    private Integer tipoArticulo;

    @Column(name = "codigo_impuesto")
    private Integer codigoImpuesto;

    @Column(name = "codigo_marca")
    private Integer idCategoria;

    @Column(name = "existencia")
    private Double existencia;

    @ManyToOne
    @JoinColumn(name = "codigo_marca", insertable = false, updatable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "codigo_impuesto", insertable = false, updatable = false)
    private Impuesto imp;

    @OneToMany(mappedBy = "art")
    private List<CodBarra> codigos;

    @OneToMany(mappedBy = "art", fetch = FetchType.EAGER)
    private List<PrecioArticulo> precioArticulos;

    @OneToMany(mappedBy = "articulo")
    private List<DetalleFactura> detalleFacturas;

    @Transient
    private Integer posicionPrecio = 0;
    @Transient
    private String codigoBarra = "";

    @Transient
    private Double precioCompra;

    @ManyToMany
    @JoinTable(name = "precios_articulos", joinColumns = @JoinColumn(name = "codigo_articulo"), inverseJoinColumns = @JoinColumn(name = "codigo_precio"))
    private Set<Precio> preciosArticulo;

    /*
     * public Set<PrecioArticulo> getPrecioArticulos() {
     * return precioArticulos;
     * }
     * 
     * public void setPrecioArticulos(Set<PrecioArticulo> precioArticulos) {
     * this.precioArticulos = precioArticulos;
     * }
     * 
     */

    /*
     * public List<PrecioArticulo> getPreciosVenta() {
     * return preciosVenta;
     * }
     * 
     * public void setPreciosVenta(List<PrecioArticulo> precios) {
     * preciosVenta = precios;
     * }
     * 
     * public void netPrecio() {
     * posicionPrecio++;
     * if (posicionPrecio == preciosVenta.size() - 1) {
     * precioVenta = preciosVenta.get(preciosVenta.size() -
     * 1).getPrecio().setScale(2, RoundingMode.HALF_EVEN).doubleValue();
     * //posicionPrecio--;
     * } else {
     * if (posicionPrecio >= preciosVenta.size()) {
     * posicionPrecio = preciosVenta.size() - 1;
     * precioVenta = preciosVenta.get(posicionPrecio).getPrecio().setScale(2,
     * RoundingMode.HALF_EVEN).doubleValue();
     * } else
     * precioVenta = preciosVenta.get(posicionPrecio).getPrecio().setScale(2,
     * RoundingMode.HALF_EVEN).doubleValue();
     * }
     * }
     * 
     * public void setPrecio(PrecioArticulo pr) {
     * int index = -1;
     * 
     * for (int c = 0; c < preciosVenta.size(); c++) {
     * 
     * if (preciosVenta.get(c).getCodigoPrecio() == pr.getCodigoPrecio()) {
     * 
     * index = c;
     * }
     * }
     * if (index != -1)
     * precioVenta = preciosVenta.get(index).getPrecio().setScale(2,
     * RoundingMode.HALF_EVEN).doubleValue();
     * }
     * 
     * public void lastPrecio() {
     * posicionPrecio--;
     * if (posicionPrecio <= 0) {
     * precioVenta = preciosVenta.get(0).getPrecio().setScale(2,
     * RoundingMode.HALF_EVEN).doubleValue();
     * posicionPrecio = 0;
     * } else {
     * //posicionPrecio--;
     * precioVenta = preciosVenta.get(posicionPrecio).getPrecio().setScale(2,
     * RoundingMode.HALF_EVEN).doubleValue();
     * }
     * }
     * 
     */

    // Constructor
    public Articulo(Integer codigoArticulo, String articulo, Integer codigoMarca, Integer codArticulo,
            Integer codigoImpuesto, Double precioArticulo, Integer tipoArticulo,
            Boolean estado, Double existencia) {
        this.articuloId = codigoArticulo;
        this.articulo = articulo;
        this.codigoImpuesto = codigoImpuesto;
        this.tipoArticulo = tipoArticulo;
        this.estado = estado;
        this.existencia = existencia;
    }

    public Articulo() {

    }

    public Integer getArticuloId() {
        return articuloId;
    }

    public void setArticuloId(Integer articuloId) {
        this.articuloId = articuloId;
    }

    public String getArticulo() {
        return articulo;
    }

    public void setArticulo(String articulo) {
        this.articulo = articulo;
    }

    public Double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(Double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public Integer getTipoArticulo() {
        return tipoArticulo;
    }

    public void setTipoArticulo(Integer tipoArticulo) {
        this.tipoArticulo = tipoArticulo;
    }

    public Integer getCodigoImpuesto() {
        return codigoImpuesto;
    }

    public void setCodigoImpuesto(Integer codigoImpuesto) {
        this.codigoImpuesto = codigoImpuesto;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Impuesto getImp() {
        return imp;
    }

    public void setImp(Impuesto imp) {
        this.imp = imp;
    }

    public List<CodBarra> getCodigos() {
        return codigos;
    }

    public void setCodigos(List<CodBarra> codigos) {
        this.codigos = codigos;
    }

    public List<PrecioArticulo> getPrecioArticulos() {
        return precioArticulos;
    }

    public void setPrecioArticulos(List<PrecioArticulo> precioArticulos) {
        this.precioArticulos = precioArticulos;
    }

    public Integer getPosicionPrecio() {
        return posicionPrecio;
    }

    public void setPosicionPrecio(Integer posicionPrecio) {
        this.posicionPrecio = posicionPrecio;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public Double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(Double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public List<DetalleFactura> getDetalleFacturas() {
        return detalleFacturas;
    }

    public void setDetalleFacturas(List<DetalleFactura> detalleFacturas) {
        this.detalleFacturas = detalleFacturas;
    }

    public Double getExistencia() {
        return existencia;
    }

    public void setExistencia(Double existencia) {
        this.existencia = existencia;
    }

    public Set<Precio> getPreciosArticulo() {
        return preciosArticulo;
    }

    public void setPreciosArticulo(Set<Precio> preciosArticulo) {
        this.preciosArticulo = preciosArticulo;
    }
}

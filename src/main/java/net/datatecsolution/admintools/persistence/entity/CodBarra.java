package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "codigos_articulos")
public class CodBarra {
    @Id
    @Column(name = "id_codigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codArticulo;
    @Column(name = "codigo_barra")
    private String codigoBarra;

    @ManyToOne
    @JoinColumn(name = "codigo_articulo", insertable = false, updatable = false)
    private Articulo art;

    public Integer getCodArticulo() {
        return codArticulo;
    }

    public void setCodArticulo(Integer codArticulo) {
        this.codArticulo = codArticulo;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public Articulo getArt() {
        return art;
    }

    public void setArt(Articulo art) {
        this.art = art;
    }
}

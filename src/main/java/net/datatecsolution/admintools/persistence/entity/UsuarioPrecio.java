package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios_precios")
public class UsuarioPrecio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "usuario", insertable = false, updatable = false)
    private Usuario use;

    @ManyToOne
    @JoinColumn(name = "codigo_precio", insertable = false, updatable = false)
    private Precio prec;

    @Column(name = "usuario")
    private String usuarioId;

    public Integer getCodigoPrecio() {
        return codigoPrecio;
    }

    public void setCodigoPrecio(Integer codigoPrecio) {
        this.codigoPrecio = codigoPrecio;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Precio getPrec() {
        return prec;
    }

    public void setPrec(Precio prec) {
        this.prec = prec;
    }

    public Usuario getUse() {
        return use;
    }

    public void setUse(Usuario use) {
        this.use = use;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name= "codigo_precio")
    private Integer codigoPrecio;




}

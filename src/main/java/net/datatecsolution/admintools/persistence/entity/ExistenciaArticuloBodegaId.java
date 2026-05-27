package net.datatecsolution.admintools.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase de PK compuesta para {@link ExistenciaArticuloBodega}.
 * Requerida por JPA cuando se usa {@code @IdClass}.
 */
public class ExistenciaArticuloBodegaId implements Serializable {

    private Integer codigoArticulo;
    private Integer codigoBodega;

    public ExistenciaArticuloBodegaId() {
    }

    public ExistenciaArticuloBodegaId(Integer codigoArticulo, Integer codigoBodega) {
        this.codigoArticulo = codigoArticulo;
        this.codigoBodega = codigoBodega;
    }

    public Integer getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(Integer codigoArticulo) { this.codigoArticulo = codigoArticulo; }
    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer codigoBodega) { this.codigoBodega = codigoBodega; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExistenciaArticuloBodegaId other)) return false;
        return Objects.equals(codigoArticulo, other.codigoArticulo)
            && Objects.equals(codigoBodega, other.codigoBodega);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoArticulo, codigoBodega);
    }
}

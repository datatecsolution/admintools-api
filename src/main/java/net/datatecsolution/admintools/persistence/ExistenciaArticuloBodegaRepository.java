package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Existencia;
import net.datatecsolution.admintools.domain.repository.ExistenciaRepository;
import net.datatecsolution.admintools.persistence.crud.ExistenciaArticuloBodegaCRUD;
import net.datatecsolution.admintools.persistence.mapper.ExistenciaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Impl de {@link ExistenciaRepository} contra la tabla materializada
 * {@code existencia_articulo_bodega}. Lecturas O(1) por PK compuesta.
 */
@Repository
public class ExistenciaArticuloBodegaRepository implements ExistenciaRepository {

    @Autowired
    private ExistenciaArticuloBodegaCRUD crud;

    @Autowired
    private ExistenciaMapper mapper;

    @Override
    public BigDecimal getExistencia(int codigoArticulo, int codigoBodega) {
        return crud.findByCodigoArticuloAndCodigoBodega(codigoArticulo, codigoBodega)
                .map(e -> e.getCantidad())
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Existencia getExistenciaDetalle(int codigoArticulo, int codigoBodega) {
        return crud.findByCodigoArticuloAndCodigoBodega(codigoArticulo, codigoBodega)
                .map(mapper::toExistencia)
                .orElse(null);
    }

    @Override
    public List<Existencia> getExistenciasPorArticulo(int codigoArticulo) {
        return mapper.toExistencias(crud.findByCodigoArticulo(codigoArticulo));
    }
}

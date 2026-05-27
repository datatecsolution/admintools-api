package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ExistenciaArticuloBodega;
import net.datatecsolution.admintools.persistence.entity.ExistenciaArticuloBodegaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExistenciaArticuloBodegaCRUD
        extends JpaRepository<ExistenciaArticuloBodega, ExistenciaArticuloBodegaId> {

    Optional<ExistenciaArticuloBodega> findByCodigoArticuloAndCodigoBodega(
            Integer codigoArticulo, Integer codigoBodega);

    List<ExistenciaArticuloBodega> findByCodigoArticulo(Integer codigoArticulo);
}

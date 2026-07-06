package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ArticuloImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticuloImagenCRUD extends JpaRepository<ArticuloImagen, Integer> {

    /** Imagen vigente del producto (la última subida, por si quedaran filas viejas). */
    Optional<ArticuloImagen> findFirstByCodigoArticuloOrderByIdImgDesc(Integer codigoArticulo);

    void deleteByCodigoArticulo(Integer codigoArticulo);
}

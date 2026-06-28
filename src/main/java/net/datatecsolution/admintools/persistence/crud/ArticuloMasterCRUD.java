package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticuloMasterCRUD extends JpaRepository<ArticuloMaster, Integer> {

    /** Busqueda paginada por nombre (LIKE %name%, case-insensitive por la collation por default). */
    Page<ArticuloMaster> findByArticuloContaining(String name, Pageable pageable);

    /** Productos de una categoría (= marca). Para el armado "Agregar la categoría". */
    Page<ArticuloMaster> findByCodigoMarca(Integer codigoMarca, Pageable pageable);

    /**
     * Búsqueda paginada por NOMBRE, CÓDIGO o CÓDIGO DE BARRA. Matchea:
     *   - articulo LIKE %term%                          (nombre)
     *   - codigo_articulo = term                        (código interno / PK)
     *   - cod_articulo = term                           (código alterno)
     *   - codigos_articulos.codigo_barra LIKE %term%    (cualquier barra del artículo)
     * Query nativo (JOIN a codigos_articulos) con DISTINCT y orden por id DESC
     * baked-in → pasar un Pageable SIN sort para no duplicar el ORDER BY.
     */
    @Query(value = """
            SELECT DISTINCT a.* FROM articulo a
            LEFT JOIN codigos_articulos c ON c.codigo_articulo = a.codigo_articulo
            WHERE a.articulo LIKE CONCAT('%', :term, '%')
               OR CAST(a.codigo_articulo AS CHAR) = :term
               OR CAST(a.cod_articulo AS CHAR) = :term
               OR c.codigo_barra LIKE CONCAT('%', :term, '%')
            ORDER BY a.codigo_articulo DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.codigo_articulo) FROM articulo a
            LEFT JOIN codigos_articulos c ON c.codigo_articulo = a.codigo_articulo
            WHERE a.articulo LIKE CONCAT('%', :term, '%')
               OR CAST(a.codigo_articulo AS CHAR) = :term
               OR CAST(a.cod_articulo AS CHAR) = :term
               OR c.codigo_barra LIKE CONCAT('%', :term, '%')
            """,
            nativeQuery = true)
    Page<ArticuloMaster> searchByNameCodeBarcode(@Param("term") String term, Pageable pageable);
}

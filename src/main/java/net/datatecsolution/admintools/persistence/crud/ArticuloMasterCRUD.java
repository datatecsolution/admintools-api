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

    /**
     * US-140 — busqueda con los CUATRO filtros de la pantalla de Productos
     * COMBINADOS: texto, categoria, estado y nivel de existencia.
     *
     * Reemplaza a las tres ramas excluyentes del service, donde el texto
     * ganaba sobre la categoria: buscar "COCA" con la categoria ABARROTERIA
     * devolvia las Coca-Cola de BEBIDAS tambien.
     *
     * Cada filtro es opcional y se apaga pasando NULL, asi una sola query
     * cubre las 16 combinaciones.
     *
     * Existencia (:stock): 'out' = sin unidades, 'low' = por debajo del
     * minimo del kardex pero con algo. Misma regla que /inventory/low-stock
     * (eab.cantidad <= ak.cantidad_minima). Un articulo SIN fila en
     * existencia_articulo_bodega cuenta como agotado — es lo que ya hacia el
     * POS al no encontrarlo en su mapa de existencias.
     *
     * Los LEFT JOIN de stock se evaluan contra UNA bodega (:warehouse). Sin
     * bodega no hay nivel de existencia que calcular, por eso el service
     * exige warehouse cuando :stock viene informado.
     */
    @Query(value = """
            SELECT DISTINCT a.* FROM articulo a
            LEFT JOIN codigos_articulos c ON c.codigo_articulo = a.codigo_articulo
            LEFT JOIN existencia_articulo_bodega eab
                   ON eab.codigo_articulo = a.codigo_articulo
                  AND eab.codigo_bodega = :warehouse
            LEFT JOIN articulo_kardex ak
                   ON ak.codigo_articulo = a.codigo_articulo
                  AND ak.codigo_bodega = :warehouse
            WHERE (:term IS NULL
                   OR a.articulo LIKE CONCAT('%', :term, '%')
                   OR CAST(a.codigo_articulo AS CHAR) = :term
                   OR CAST(a.cod_articulo AS CHAR) = :term
                   OR c.codigo_barra LIKE CONCAT('%', :term, '%'))
              AND (:category IS NULL OR a.codigo_marca = :category)
              AND (:active   IS NULL OR a.estado = :active)
              AND (:stock    IS NULL
                   OR a.tipo_articulo = 2
                   OR (:stock = 'out' AND IFNULL(eab.cantidad, 0) <= 0)
                   OR (:stock = 'low' AND IFNULL(eab.cantidad, 0) > 0
                                      AND IFNULL(eab.cantidad, 0) <= IFNULL(ak.cantidad_minima, 0)))
            ORDER BY a.codigo_articulo DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.codigo_articulo) FROM articulo a
            LEFT JOIN codigos_articulos c ON c.codigo_articulo = a.codigo_articulo
            LEFT JOIN existencia_articulo_bodega eab
                   ON eab.codigo_articulo = a.codigo_articulo
                  AND eab.codigo_bodega = :warehouse
            LEFT JOIN articulo_kardex ak
                   ON ak.codigo_articulo = a.codigo_articulo
                  AND ak.codigo_bodega = :warehouse
            WHERE (:term IS NULL
                   OR a.articulo LIKE CONCAT('%', :term, '%')
                   OR CAST(a.codigo_articulo AS CHAR) = :term
                   OR CAST(a.cod_articulo AS CHAR) = :term
                   OR c.codigo_barra LIKE CONCAT('%', :term, '%'))
              AND (:category IS NULL OR a.codigo_marca = :category)
              AND (:active   IS NULL OR a.estado = :active)
              AND (:stock    IS NULL
                   OR a.tipo_articulo = 2
                   OR (:stock = 'out' AND IFNULL(eab.cantidad, 0) <= 0)
                   OR (:stock = 'low' AND IFNULL(eab.cantidad, 0) > 0
                                      AND IFNULL(eab.cantidad, 0) <= IFNULL(ak.cantidad_minima, 0)))
            """,
            nativeQuery = true)
    Page<ArticuloMaster> searchFiltered(@Param("term") String term,
                                        @Param("category") Integer category,
                                        @Param("active") Integer active,
                                        @Param("stock") String stock,
                                        @Param("warehouse") Integer warehouse,
                                        Pageable pageable);
}

package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ArticuloCRUD extends JpaRepository<Articulo, Integer> {

    List<Articulo> findByIdCategoriaOrderByArticuloAsc(int idCategoria);

    List<Articulo> findByArticuloLikeOrderByArticuloAsc(String descripcion);

    @Query(value = "Select articulo.codigo_articulo, " +
            " articulo.articulo, " +
            " articulo.codigo_marca, " +
            " articulo.cod_articulo, " +
            " articulo.codigo_impuesto,  " +
            " articulo.precio_articulo, " +
            " articulo.tipo_articulo,  " +
            " articulo.estado, " +
            " IFNULL( f_can_saldo_kardex ( articulo.codigo_articulo, 1 ), 0 ) AS existencia from articulo where articulo like ? AND articulo.estado = 1",nativeQuery = true)
    List<Articulo> getArticuloDescripcion(String nombre);

    List<Articulo> findByArticuloContaining(String descripcion);

//    @Query(value = "SELECT a.codigo_articulo, " +
//            "a.articulo, " +
//            "a.codigo_marca, " +
//            "a.cod_articulo, " +
//            "a.codigo_impuesto, " +
//            "a.precio_articulo, " +
//            "a.tipo_articulo, " +
//            "a.estado, " +
//            "IFNULL(f_can_saldo_kardex(a.codigo_articulo, 1), 0) AS existencia " +
//            "FROM articulo a " +
//            "WHERE a.articulo LIKE %:description% AND a.estado = 1",
//            nativeQuery = true)
//    List<Tuple> getArticuloDescripcionUsuario(@Param("description") String description);



    @Query(value = "Select a.codigo_articulo, " +
            " a.articulo, " +
            " a.codigo_marca, " +
            " a.cod_articulo, " +
            " a.codigo_impuesto,  " +
            " a.precio_articulo, " +
            " a.tipo_articulo,  " +
            " a.estado, " +
            " a.existencia " +
            " from articulo_view a " +
            " JOIN precios_articulos pre ON a.codigo_articulo = pre.codigo_articulo  " +
            " JOIN usuarios_precios user ON pre.codigo_precio = user.codigo_precio " +
            " where a.articulo like ? AND a.estado = 1 AND user.usuario=? GROUP BY a.codigo_articulo",nativeQuery = true)
    List<Articulo>findProductosByUsuarioAndNombre(String articulo, String usuario);





    @Query(value ="Select p.codigo_articulo, " +
            " p.articulo, " +
            " p.codigo_marca, " +
            " p.cod_articulo, " +
            " p.codigo_impuesto,  " +
            " p.precio_articulo, " +
            " p.tipo_articulo,  " +
            " p.estado, " +
            " p.existencia " +
            " FROM articulo_view p JOIN precios_articulos pp ON p.codigo_articulo = pp.codigo_articulo JOIN precios pr  ON pp.codigo_precio = pr.codigo_precio JOIN usuarios_precios up ON pr.codigo_precio = up.codigo_precio WHERE p.articulo LIKE %:descripcion% AND up.usuario = :userId GROUP BY p.codigo_articulo",nativeQuery = true)
    List<Articulo> findByDescripcionAndUser(@Param("descripcion") String descripcion, @Param("userId") String userId);







    @Query(value = "Select * from articulo where codigo_marca=?",nativeQuery = true)
    List<Articulo> getArticuloCategoria(int idCategoria);

    /**
     * US-074: lock pesimista de las filas base de articulo para serializar
     * los saves de órdenes concurrentes. Los ids deben venir SIEMPRE en orden
     * ascendente (anti-deadlock). El cálculo de disponible va en el statement
     * SIGUIENTE y requiere que la tx corra en READ_COMMITTED (ver
     * OrderService.save): así toma snapshot fresco después de adquirir el
     * lock y ve lo que commiteó la transacción competidora.
     */
    @Query(value = "SELECT a.codigo_articulo FROM articulo a " +
            "WHERE a.codigo_articulo IN (:ids) ORDER BY a.codigo_articulo FOR UPDATE",
            nativeQuery = true)
    List<Integer> lockByIds(@Param("ids") List<Integer> ids);

    /**
     * US-074/US-109: disponible real por artículo = f_existencia_y_ordenes
     * (saldo kardex − órdenes pendientes) en la BODEGA del vendedor (US-109:
     * antes hardcodeada a 1) + add-back de las líneas de la propia orden
     * cuando es update (la función ya las cuenta como pendientes; el subquery
     * replica sus condiciones: estado IN (1,2) — US-121: solo Activa y
     * Modificada reservan — y caja de esa bodega). Solo
     * artículos CON kardex en la bodega: sin kardex no hay control de stock
     * (misma regla que el trigger del Swing).
     */
    @Query(value = "SELECT a.codigo_articulo, a.articulo, " +
            "IFNULL(f_existencia_y_ordenes(a.codigo_articulo, :bodega), 0) " +
            "+ IFNULL((SELECT SUM(d.cantidad) " +
            "            FROM detalle_factura_temp d " +
            "            JOIN encabezado_factura_temp e ON e.numero_factura = d.numero_factura " +
            "            JOIN cajas c ON e.codigo_caja = c.codigo " +
            "           WHERE d.codigo_articulo = a.codigo_articulo " +
            "             AND d.numero_factura = :ordenExcluida " +
            "             AND c.codigo_bodega = :bodega " +
            "             AND e.estado IN (1, 2)), 0) AS disponible " +
            "FROM articulo a " +
            "WHERE a.codigo_articulo IN (:ids) " +
            "AND EXISTS (SELECT 1 FROM articulo_kardex ak " +
            "             WHERE ak.codigo_articulo = a.codigo_articulo " +
            "               AND ak.codigo_bodega = :bodega)",
            nativeQuery = true)
    List<Object[]> findDisponibleParaOrden(@Param("ids") List<Integer> ids,
                                           @Param("ordenExcluida") int ordenExcluida,
                                           @Param("bodega") int bodega);

    // Optional<List<Producto>> findByCantidadStockLessThanAndEstado(int cantidadStock, boolean estado);
/*
    @Query(value = "Select * from articulo where codigo_marca=?",nativeQuery = true)
    List<Articulo> getArticuloCategoria(int idCategoria);

 */
}

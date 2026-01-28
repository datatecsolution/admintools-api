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



    // Optional<List<Producto>> findByCantidadStockLessThanAndEstado(int cantidadStock, boolean estado);
/*
    @Query(value = "Select * from articulo where codigo_marca=?",nativeQuery = true)
    List<Articulo> getArticuloCategoria(int idCategoria);

 */
}

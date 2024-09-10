package net.datatecsolution.admintools.persistence.crud;

import jakarta.persistence.Tuple;
import net.datatecsolution.admintools.persistence.entity.Articulo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticuloCRUD extends CrudRepository<Articulo, Integer> {

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
//    List<Tuple> getArticuloDescripcion(@Param("description") String description);




    @Query(value = "Select * from articulo where codigo_marca=?",nativeQuery = true)
    List<Articulo> getArticuloCategoria(int idCategoria);

    // Optional<List<Producto>> findByCantidadStockLessThanAndEstado(int cantidadStock, boolean estado);
/*
    @Query(value = "Select * from articulo where codigo_marca=?",nativeQuery = true)
    List<Articulo> getArticuloCategoria(int idCategoria);

 */
}

package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreciosArticuloCRUD extends JpaRepository<PrecioArticulo, Integer> {

    @Query(value = "SELECT precios_articulos.codigo_articulo, " +
            " precios_articulos.precio_articulo, " +
            " precios_articulos.codigo_precio, " +
            " precios_articulos.id " +
            " FROM precios_articulos " +
            " INNER JOIN precios ON precios_articulos.codigo_precio=precios.codigo_precio " +
            " INNER JOIN usuarios_precios ON precios.codigo_precio=usuarios_precios.codigo_precio " +
            " WHERE precios_articulos.codigo_articulo=? AND usuarios_precios.usuario=?",nativeQuery = true)
    List<PrecioArticulo> findPrecioUser(Integer articuloId, String usuario);

}

package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ArticuloKardex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * US-035 — Lectura del kardex: configuracion por (articulo, bodega), historial
 * de movimientos, valoracion (promedio ponderado via {@code f_precio_saldo_kardex})
 * y alertas de stock minimo. Queries nativas con proyecciones; las columnas se
 * aliasan al nombre de la propiedad para que Spring Data las mapee.
 */
public interface ArticuloKardexCRUD extends JpaRepository<ArticuloKardex, Integer> {

    Optional<ArticuloKardex> findByCodigoArticuloAndCodigoBodega(int codigoArticulo, int codigoBodega);

    /** Historial de movimientos de un kardex (mas reciente primero). */
    @Query(value = "SELECT dmk.codigo_movimiento AS codigoMovimiento, " +
            " dmk.fecha AS fecha, " +
            " mk.codigo_tipo_movimiento AS tipoMovimiento, " +
            " tmk.movimiento AS tipoMovimientoDesc, " +
            " dmk.descripcion AS descripcion, " +
            " dmk.no_documento AS documento, " +
            " mk.cantidad AS cantidad, " +
            " mk.precio_unidad AS precioUnidad, " +
            " mk.total AS total " +
            " FROM detalle_movimiento_kardex dmk " +
            " JOIN movimiento_kardex mk ON mk.codigo_movimiento = dmk.codigo_movimiento " +
            " LEFT JOIN tipo_movimiento_kardex tmk ON tmk.codigo_tipo_moviemiento = mk.codigo_tipo_movimiento " +
            " WHERE dmk.codigo_kardex = :codigoKardex " +
            " ORDER BY dmk.codigo_movimiento DESC",
            countQuery = "SELECT COUNT(*) FROM detalle_movimiento_kardex dmk " +
            " JOIN movimiento_kardex mk ON mk.codigo_movimiento = dmk.codigo_movimiento " +
            " WHERE dmk.codigo_kardex = :codigoKardex",
            nativeQuery = true)
    Page<KardexMovementView> findMovements(@Param("codigoKardex") int codigoKardex, Pageable pageable);

    /**
     * Valoracion por (articulo, bodega): cantidad x costo promedio.
     * US-112: + reservado (v_reservado_por_articulo, pedidos estado &lt; 3) y
     * disponible = cantidad − reservado. La valoracion sigue sobre FISICO.
     */
    @Query(value = "SELECT eab.codigo_articulo AS codigoArticulo, " +
            " a.articulo AS articulo, " +
            " eab.codigo_bodega AS codigoBodega, " +
            " eab.cantidad AS cantidad, " +
            " IFNULL(f_precio_saldo_kardex(ak.codigo_kardex), 0) AS costoUnitario, " +
            " IFNULL(eab.cantidad * f_precio_saldo_kardex(ak.codigo_kardex), 0) AS valorTotal, " +
            " IFNULL(r.reservado, 0) AS reservado, " +
            " eab.cantidad - IFNULL(r.reservado, 0) AS disponible " +
            " FROM existencia_articulo_bodega eab " +
            " JOIN articulo a ON a.codigo_articulo = eab.codigo_articulo " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " LEFT JOIN v_reservado_por_articulo r ON r.codigo_articulo = eab.codigo_articulo AND r.codigo_bodega = eab.codigo_bodega " +
            " WHERE (:warehouse IS NULL OR eab.codigo_bodega = :warehouse) " +
            "   AND (:name IS NULL OR a.articulo LIKE CONCAT('%', :name, '%')) " +
            " ORDER BY a.articulo ASC",
            countQuery = "SELECT COUNT(*) FROM existencia_articulo_bodega eab " +
            " JOIN articulo a ON a.codigo_articulo = eab.codigo_articulo " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " WHERE (:warehouse IS NULL OR eab.codigo_bodega = :warehouse) " +
            "   AND (:name IS NULL OR a.articulo LIKE CONCAT('%', :name, '%'))",
            nativeQuery = true)
    Page<StockValuationView> findValuation(@Param("warehouse") Integer warehouse,
                                           @Param("name") String name,
                                           Pageable pageable);

    /**
     * US-141 — existencia de un CONJUNTO de articulos en una bodega.
     *
     * Misma aritmetica que findValuation (cantidad, costo promedio via
     * f_precio_saldo_kardex, reservado y disponible), pero acotada por
     * codigo_articulo IN (:ids): una query por pagina de productos en vez de
     * traer la valoracion entera.
     *
     * Devuelve tambien cantidad_minima para que el consumidor calcule el
     * nivel (ok/bajo/agotado) sin pedir low-stock aparte.
     *
     * Los articulos SIN fila en existencia_articulo_bodega no vienen en el
     * resultado: el llamador los interpreta como existencia 0, que es lo que
     * significan.
     */
    @Query(value = "SELECT eab.codigo_articulo AS codigoArticulo, " +
            " eab.cantidad AS cantidad, " +
            " IFNULL(f_precio_saldo_kardex(ak.codigo_kardex), 0) AS costoUnitario, " +
            " IFNULL(r.reservado, 0) AS reservado, " +
            " eab.cantidad - IFNULL(r.reservado, 0) AS disponible, " +
            " IFNULL(ak.cantidad_minima, 0) AS cantidadMinima " +
            " FROM existencia_articulo_bodega eab " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " LEFT JOIN v_reservado_por_articulo r ON r.codigo_articulo = eab.codigo_articulo AND r.codigo_bodega = eab.codigo_bodega " +
            " WHERE eab.codigo_bodega = :warehouse AND eab.codigo_articulo IN (:ids)",
            nativeQuery = true)
    List<ProductStockView> findStockByProducts(@Param("warehouse") int warehouse,
                                               @Param("ids") List<Integer> ids);

    /** US-112: reservado puntual de un articulo en una bodega (0 si no hay pedidos). */
    @Query(value = "SELECT IFNULL(reservado, 0) FROM v_reservado_por_articulo " +
            " WHERE codigo_articulo = :articulo AND codigo_bodega = :bodega",
            nativeQuery = true)
    Optional<BigDecimal> findReservado(@Param("articulo") int articulo, @Param("bodega") int bodega);

    /** US-112: reservado de un articulo en TODAS las bodegas. Fila = [codigo_bodega, reservado]. */
    @Query(value = "SELECT codigo_bodega, reservado FROM v_reservado_por_articulo " +
            " WHERE codigo_articulo = :articulo",
            nativeQuery = true)
    List<Object[]> findReservadoPorBodega(@Param("articulo") int articulo);

    /** Valor total del inventario (opcionalmente filtrado por bodega). */
    @Query(value = "SELECT IFNULL(SUM(eab.cantidad * IFNULL(f_precio_saldo_kardex(ak.codigo_kardex), 0)), 0) " +
            " FROM existencia_articulo_bodega eab " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " WHERE (:warehouse IS NULL OR eab.codigo_bodega = :warehouse)",
            nativeQuery = true)
    BigDecimal sumValuation(@Param("warehouse") Integer warehouse);

    /** Articulos con existencia <= cantidad_minima (alerta de stock minimo). */
    @Query(value = "SELECT eab.codigo_articulo AS codigoArticulo, " +
            " a.articulo AS articulo, " +
            " eab.codigo_bodega AS codigoBodega, " +
            " eab.cantidad AS cantidad, " +
            " ak.cantidad_minima AS cantidadMinima, " +
            " (ak.cantidad_minima - eab.cantidad) AS faltante " +
            " FROM existencia_articulo_bodega eab " +
            " JOIN articulo a ON a.codigo_articulo = eab.codigo_articulo " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " WHERE eab.cantidad <= ak.cantidad_minima " +
            "   AND (:warehouse IS NULL OR eab.codigo_bodega = :warehouse) " +
            "   AND (:name IS NULL OR a.articulo LIKE CONCAT('%', :name, '%')) " +
            " ORDER BY (ak.cantidad_minima - eab.cantidad) DESC",
            countQuery = "SELECT COUNT(*) FROM existencia_articulo_bodega eab " +
            " JOIN articulo a ON a.codigo_articulo = eab.codigo_articulo " +
            " JOIN articulo_kardex ak ON ak.codigo_articulo = eab.codigo_articulo AND ak.codigo_bodega = eab.codigo_bodega " +
            " WHERE eab.cantidad <= ak.cantidad_minima " +
            "   AND (:warehouse IS NULL OR eab.codigo_bodega = :warehouse) " +
            "   AND (:name IS NULL OR a.articulo LIKE CONCAT('%', :name, '%'))",
            nativeQuery = true)
    Page<LowStockView> findLowStock(@Param("warehouse") Integer warehouse,
                                    @Param("name") String name,
                                    Pageable pageable);
}

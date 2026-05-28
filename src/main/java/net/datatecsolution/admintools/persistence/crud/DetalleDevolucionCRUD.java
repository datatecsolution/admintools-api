package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleDevolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de devoluciones de venta (admin_tools.detalle_devoluciones).
 * Common EMF — no es per-caja.
 */
public interface DetalleDevolucionCRUD extends JpaRepository<DetalleDevolucion, Integer> {

    List<DetalleDevolucion> findByNumeroFacturaAndCodigoCaja(Integer numeroFactura, Integer codigoCaja);

    @Query(value = "SELECT d FROM DetalleDevolucion d WHERE " +
            "(:numeroFactura IS NULL OR d.numeroFactura = :numeroFactura) AND " +
            "(:from IS NULL OR d.fecha >= :from) AND " +
            "(:to   IS NULL OR d.fecha <= :to)")
    Page<DetalleDevolucion> search(@Param("numeroFactura") Integer numeroFactura,
                                   @Param("from") LocalDate from,
                                   @Param("to")   LocalDate to,
                                   Pageable pageable);

    /**
     * Suma de cantidad ya devuelta para un articulo en una factura+caja.
     * Sirve para validar "no devolver mas de lo facturado" descontando
     * devoluciones previas. Devuelve 0 si no hay filas.
     */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleDevolucion d " +
           "WHERE d.numeroFactura = :numeroFactura " +
           "  AND d.codigoCaja    = :codigoCaja " +
           "  AND d.codigoArticulo = :codigoArticulo")
    BigDecimal sumCantidadByFacturaCajaArticulo(@Param("numeroFactura") Integer numeroFactura,
                                                @Param("codigoCaja")    Integer codigoCaja,
                                                @Param("codigoArticulo") Integer codigoArticulo);
}

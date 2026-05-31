package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ConfigApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * US-034 / US-031 — Configuracion global de facturacion (BD comun, single-row,
 * sin PK real). Lectura y actualizacion via queries nativas.
 */
public interface ConfigAppCRUD extends JpaRepository<ConfigApp, Integer> {

    /** Dias de credito por defecto (igual que el Swing: SELECT ... LIMIT 1). */
    @Query(value = "SELECT dia_vencimiento_factura FROM config_app LIMIT 1", nativeQuery = true)
    Optional<Integer> findDiaVencimientoFactura();

    /** US-031 — interes por facturas vencidas. */
    @Query(value = "SELECT interes_para_facturas_venc FROM config_app LIMIT 1", nativeQuery = true)
    Optional<Integer> findInteresFacturasVenc();

    /** US-031 — actualiza la (unica) fila de config. Devuelve filas afectadas. */
    @Modifying
    @Query(value = "UPDATE config_app SET dia_vencimiento_factura = :dias, " +
            " interes_para_facturas_venc = :interes", nativeQuery = true)
    int updateConfig(@Param("dias") int dias, @Param("interes") int interes);

    /** US-031 — inserta la fila inicial si la tabla esta vacia. */
    @Modifying
    @Query(value = "INSERT INTO config_app (dia_vencimiento_factura, interes_para_facturas_venc) " +
            " VALUES (:dias, :interes)", nativeQuery = true)
    int insertConfig(@Param("dias") int dias, @Param("interes") int interes);
}

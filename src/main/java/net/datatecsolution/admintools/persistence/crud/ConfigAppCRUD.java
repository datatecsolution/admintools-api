package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ConfigApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * US-034 — Lectura de la configuracion global de facturacion (BD comun).
 */
public interface ConfigAppCRUD extends JpaRepository<ConfigApp, Integer> {

    /** Dias de credito por defecto (igual que el Swing: SELECT ... LIMIT 1). */
    @Query(value = "SELECT dia_vencimiento_factura FROM config_app LIMIT 1", nativeQuery = true)
    Optional<Integer> findDiaVencimientoFactura();
}

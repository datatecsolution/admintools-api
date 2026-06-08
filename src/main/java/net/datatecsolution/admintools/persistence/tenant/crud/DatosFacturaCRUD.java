package net.datatecsolution.admintools.persistence.tenant.crud;

import net.datatecsolution.admintools.persistence.tenant.entity.DatosFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Checkout POS — acceso al rango de facturación CAI de la caja (BD tenant).
 */
public interface DatosFacturaCRUD extends JpaRepository<DatosFactura, Integer> {

    /** Rango/CAI activo = el de mayor codigo_rango (mirror de getIdCaiAct). */
    Optional<DatosFactura> findTopByOrderByCodigoRangoDesc();
}

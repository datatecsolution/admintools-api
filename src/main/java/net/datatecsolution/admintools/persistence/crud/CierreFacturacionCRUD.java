package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CierreFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Rangos de facturas por caja dentro de un cierre. Mirrors de
 * {@code CierreFacturacionDao.buscarPorCajaUsuario(...)} del Swing.
 */
public interface CierreFacturacionCRUD extends JpaRepository<CierreFacturacion, Integer> {

    /** Ultimo rango registrado para caja+usuario (de cualquier cierre). */
    Optional<CierreFacturacion> findFirstByUsuarioAndCodigoCajaOrderByIdDesc(String usuario, Integer codigoCaja);

    /** Rango de una caja dentro de un cierre especifico. */
    Optional<CierreFacturacion> findFirstByUsuarioAndCodigoCierreAndCodigoCaja(String usuario, Integer codigoCierre, Integer codigoCaja);

    /**
     * US-149: ultimo rango COMPLETADO (factura_final > 0) para caja+usuario,
     * excluyendo un cierre (el turno en curso). Para detectar aperturas que
     * quedaron con factura_inicial envenenada.
     */
    Optional<CierreFacturacion> findFirstByUsuarioAndCodigoCajaAndCodigoCierreNotAndFacturaFinalGreaterThanOrderByIdDesc(
            String usuario, Integer codigoCaja, Integer codigoCierre, Integer facturaFinal);

    List<CierreFacturacion> findByCodigoCierre(Integer codigoCierre);
}

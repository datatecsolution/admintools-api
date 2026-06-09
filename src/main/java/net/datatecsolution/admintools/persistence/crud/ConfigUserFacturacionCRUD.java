package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.ConfigUserFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Config de facturación por usuario (config_user_facturacion).
 */
public interface ConfigUserFacturacionCRUD extends JpaRepository<ConfigUserFacturacion, Integer> {

    /** ¿Mostrar/forzar selección de vendedor al facturar? (1=sí). */
    @Query(value = "SELECT ventana_vendedor FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findVentanaVendedor(@Param("usuario") String usuario);

    /** ¿Pedir observaciones de la factura al cobrar? (1=sí). */
    @Query(value = "SELECT ventana_observaciones FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findVentanaObservaciones(@Param("usuario") String usuario);

    /** ¿El descuento se ingresa en porcentaje? (1=%, 0=monto en L). */
    @Query(value = "SELECT descuento_porcentaje FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findDescuentoPorcentaje(@Param("usuario") String usuario);

    /** ¿"Precios" pide contraseña de admin? (1=sí). isPwdPrecio del Swing. */
    @Query(value = "SELECT pwd_precio FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findPwdPrecio(@Param("usuario") String usuario);

    /** ¿"Descuentos" pide contraseña de admin? (1=sí). isPwdDescuento del Swing. */
    @Query(value = "SELECT pwd_descuento FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findPwdDescuento(@Param("usuario") String usuario);
}

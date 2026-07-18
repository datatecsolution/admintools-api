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

    @Query(value = "SELECT crear_cliente_credito FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findCrearClienteCredito(@Param("usuario") String usuario);

    /**
     * ¿Puede vender sin inventario? (1=permite sobreventa, 0=bloquea).
     * Sin row → permitir (histórico). US-074: misma semántica opt-in que
     * el SP crear_venta_kardex_v2 (V33) aplica al facturar.
     */
    @Query(value = "SELECT facturar_sin_inventario FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findFacturarSinInventario(@Param("usuario") String usuario);

    /**
     * ¿Rotación automática de cajas en ventas a consumidor final? (1=sí).
     * Sin row o 0 → APAGADO (opt-in explícito, como el checkbox del Swing).
     * US-102: el +0 fuerza tipo numérico — la columna es boolean/tinyint(1)
     * y el driver la devolvería como Boolean, rompiendo Optional&lt;Integer&gt;.
     */
    @Query(value = "SELECT rotacion_automatica_cajas+0 FROM config_user_facturacion WHERE usuario = :usuario LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findRotacionAutomaticaCajas(@Param("usuario") String usuario);
}

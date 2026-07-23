package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.SellerResponse;
import net.datatecsolution.admintools.domain.dto.SellerSettingsResponse;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.EmpleadoCRUD;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Config de vendedor para el POS: expone el catálogo de vendedores (empleados)
 * y el flag {@code ventana_vendedor} del usuario, que decide si se debe elegir
 * vendedor al facturar (igual que el Swing). Default de vendedor = 1.
 */
@Service
public class SellerCatalogService {

    private final EmpleadoCRUD empleadoCRUD;
    private final ConfigUserFacturacionCRUD configUserFacturacionCRUD;

    public SellerCatalogService(EmpleadoCRUD empleadoCRUD,
                                ConfigUserFacturacionCRUD configUserFacturacionCRUD) {
        this.empleadoCRUD = empleadoCRUD;
        this.configUserFacturacionCRUD = configUserFacturacionCRUD;
    }

    public boolean isVentanaVendedor(String user) {
        return configUserFacturacionCRUD.findVentanaVendedor(user).orElse(0) == 1;
    }

    public SellerSettingsResponse getSettings(String user) {
        boolean ventanaObservaciones =
                configUserFacturacionCRUD.findVentanaObservaciones(user).orElse(0) == 1;
        boolean descuentoEnPorcentaje =
                configUserFacturacionCRUD.findDescuentoPorcentaje(user).orElse(0) == 1;
        boolean pwdPrecio =
                configUserFacturacionCRUD.findPwdPrecio(user).orElse(0) == 1;
        boolean pwdDescuento =
                configUserFacturacionCRUD.findPwdDescuento(user).orElse(0) == 1;
        List<SellerResponse> sellers = empleadoCRUD.findAll().stream()
                .map(e -> new SellerResponse(
                        e.getCodigo(),
                        (e.getNombre() + " " + e.getApellido()).trim()))
                .toList();
        // US-105: con la rotación automática (US-102) encendida el POS oculta
        // el selector manual de caja.
        boolean rotacionAutomatica =
                configUserFacturacionCRUD.findRotacionAutomaticaCajas(user).orElse(0) == 1;
        return new SellerSettingsResponse(
                isVentanaVendedor(user), ventanaObservaciones, descuentoEnPorcentaje,
                pwdPrecio, pwdDescuento, puedeCrearClienteCredito(user), sellers,
                rotacionAutomatica);
    }

    /** V30: el cajero puede crear clientes de crédito (tipo 2) desde el POS. */
    public boolean puedeCrearClienteCredito(String user) {
        return configUserFacturacionCRUD.findCrearClienteCredito(user).orElse(0) == 1;
    }

    /**
     * Resuelve el codigo_vendedor de una factura segun el Swing: si
     * ventana_vendedor=1 se usa el seleccionado (vendedorId), sino default 1.
     */
    public int resolveVendedor(String user, Integer vendedorId) {
        if (isVentanaVendedor(user) && vendedorId != null && vendedorId >= 1) {
            return vendedorId;
        }
        return 1;
    }
}

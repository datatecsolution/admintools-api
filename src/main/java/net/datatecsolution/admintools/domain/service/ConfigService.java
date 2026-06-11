package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.ConfigRequest;
import net.datatecsolution.admintools.domain.dto.ConfigResponse;
import net.datatecsolution.admintools.persistence.crud.ConfigAppCRUD;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-031 — Parametros generales de facturacion (config_app, single-row sin PK).
 * Lectura/escritura via queries nativas en {@link ConfigAppCRUD}.
 */
@Service
public class ConfigService {

    private final ConfigAppCRUD crud;

    public ConfigService(ConfigAppCRUD crud) {
        this.crud = crud;
    }

    /** Ventana por defecto del ranking de mas vendidos si la config no la trae. */
    public static final int DEFAULT_DIAS_RANKING = 30;

    public ConfigResponse getConfig() {
        return new ConfigResponse(
                crud.findDiaVencimientoFactura().orElse(0),
                crud.findInteresFacturasVenc().orElse(0),
                crud.findDiasRankingMasVendidos().orElse(DEFAULT_DIAS_RANKING));
    }

    /** Solo la ventana del ranking (para SalesRankingService). */
    public int getDiasRankingMasVendidos() {
        return crud.findDiasRankingMasVendidos().orElse(DEFAULT_DIAS_RANKING);
    }

    @Transactional
    public ConfigResponse updateConfig(ConfigRequest req) {
        int rows = crud.updateConfig(req.diaVencimientoFactura(), req.interesParaFacturasVenc(),
                req.diasRankingMasVendidos());
        if (rows == 0) {
            // tabla vacia (single-row aun no inicializada)
            crud.insertConfig(req.diaVencimientoFactura(), req.interesParaFacturasVenc(),
                    req.diasRankingMasVendidos());
        }
        return new ConfigResponse(req.diaVencimientoFactura(), req.interesParaFacturasVenc(),
                req.diasRankingMasVendidos());
    }
}

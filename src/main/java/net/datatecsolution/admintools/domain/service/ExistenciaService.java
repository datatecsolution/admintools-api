package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Existencia;
import net.datatecsolution.admintools.domain.dto.ExistenciaResponse;
import net.datatecsolution.admintools.domain.repository.ExistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de lectura de stock (INV-1). Lee detras de
 * {@link ExistenciaRepository}, que hoy va contra la tabla materializada
 * {@code existencia_articulo_bodega}.
 *
 * Convierte el POJO de dominio {@link Existencia} al DTO {@link ExistenciaResponse}.
 */
@Service
public class ExistenciaService {

    @Autowired
    private ExistenciaRepository existenciaRepository;

    /**
     * Saldo de un articulo en una bodega especifica.
     * Si la combinacion no tiene registro en la tabla de saldos, devuelve
     * cantidad 0 (semantica: "stock cero, no error"). descripcionBodega
     * solo se rellena cuando si hay registro (con el JOIN a bodega).
     */
    public ExistenciaResponse getExistencia(int codigoArticulo, int codigoBodega) {
        Existencia e = existenciaRepository.getExistenciaDetalle(codigoArticulo, codigoBodega);
        if (e == null) {
            return new ExistenciaResponse(codigoArticulo, codigoBodega, null, BigDecimal.ZERO);
        }
        return new ExistenciaResponse(
                e.getCodigoArticulo(), e.getCodigoBodega(),
                e.getDescripcionBodega(), e.getCantidad());
    }

    /** Saldos del articulo en TODAS las bodegas donde tiene kardex. */
    public List<ExistenciaResponse> getExistenciasPorArticulo(int codigoArticulo) {
        return existenciaRepository.getExistenciasPorArticulo(codigoArticulo).stream()
                .map(e -> new ExistenciaResponse(
                        e.getCodigoArticulo(),
                        e.getCodigoBodega(),
                        e.getDescripcionBodega(),
                        e.getCantidad()))
                .toList();
    }
}

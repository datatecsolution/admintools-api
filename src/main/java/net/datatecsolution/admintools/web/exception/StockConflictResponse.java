package net.datatecsolution.admintools.web.exception;

import net.datatecsolution.admintools.domain.dto.StockConflict;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo del 409 de sobreventa (US-074): como {@link ApiErrorResponse} pero
 * con el detalle estructurado por producto para que el front pueda mostrar
 * pedida vs disponible por línea.
 */
public record StockConflictResponse(
        Instant timestamp,
        int status,
        String message,
        List<StockConflict> conflicts
) {
    public StockConflictResponse(int status, String message, List<StockConflict> conflicts) {
        this(Instant.now(), status, message, conflicts);
    }
}

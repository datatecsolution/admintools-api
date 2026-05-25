package net.datatecsolution.admintools.web.exception;

import java.time.Instant;
import java.util.List;

/**
 * Forma estandar de error que devuelve el API para todo el stack.
 * Lo produce {@code GlobalExceptionHandler}.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String message,
        List<String> details
) {
    public ApiErrorResponse(int status, String message, List<String> details) {
        this(Instant.now(), status, message, details);
    }
}

package net.datatecsolution.admintools.domain.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida del master de producto (INV-4). Datos maestros sin stock —
 * el stock se consulta aparte por {@code GET /inventory/stock}.
 *
 * imageVersion (US-079): id_img vigente en articulo_imagen, o null si el
 * producto no tiene imagen. El front arma GET /products/{id}/image?v={version}
 * (caché inmutable; cambiar la imagen cambia la versión y rompe el caché).
 */
public record ProductResponse(
        Integer id,
        String name,
        BigDecimal price,
        Integer categoryId,
        Integer taxId,
        Integer altCode,
        Integer type,
        Boolean active,
        List<String> barcodes,
        Integer imageVersion
) {
}

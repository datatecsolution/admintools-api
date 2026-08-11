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
        Integer imageVersion,
        /**
         * US-141 — existencia en la bodega consultada, o null si la peticion
         * no indico bodega (o si es un alta/edicion, donde no aplica).
         *
         * Antes el listado no traia stock y el POS lo resolvia pidiendo la
         * valoracion COMPLETA con size=1000: con catalogos mayores a ese
         * tope, los articulos que quedaban afuera se mostraban en cero.
         */
        ProductStock stock
) {
    /** Copia con el stock adjunto (el mapper construye el resto). */
    public ProductResponse conStock(ProductStock s) {
        return new ProductResponse(id, name, price, categoryId, taxId, altCode,
                type, active, barcodes, imageVersion, s);
    }
}

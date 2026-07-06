package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.service.ProductImageService;
import net.datatecsolution.admintools.persistence.entity.ArticuloImagen;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

/**
 * US-079 — imagen del producto (blob en `articulo_imagen`, ver
 * ProductImageService). El GET es público (los &lt;img src&gt; no mandan JWT,
 * igual que /uploads/**) y se sirve con caché inmutable: el POS versiona la
 * URL con ?v={imageVersion} para romper caché al reemplazar.
 */
@RestController
@RequestMapping("/products/{productId}/image")
@Tag(name = "Productos", description = "Imagen del producto")
public class ProductImageCtl {

    private final ProductImageService service;

    public ProductImageCtl(ProductImageService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sube/reemplaza la imagen del producto (jpeg/png/webp ≤5MB → medium JPEG)")
    public ResponseEntity<Map<String, Integer>> upload(@PathVariable int productId,
                                                       @RequestPart("file") MultipartFile file) {
        int version = service.store(productId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("imageVersion", version));
    }

    @GetMapping
    @Operation(summary = "Imagen del producto (pública, caché inmutable + ETag)")
    public ResponseEntity<byte[]> get(@PathVariable int productId,
                                      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return service.get(productId)
                .map(img -> {
                    String etag = "\"" + img.getIdImg() + "\"";
                    if (etag.equals(ifNoneMatch)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).<byte[]>build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                            .contentType(contentType(img))
                            .body(img.getImg());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Quita la imagen del producto")
    public ResponseEntity<Void> delete(@PathVariable int productId) {
        service.delete(productId);
        return ResponseEntity.noContent().build();
    }

    private static MediaType contentType(ArticuloImagen img) {
        return switch (img.getExtension() == null ? "" : img.getExtension().toLowerCase()) {
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
    }
}

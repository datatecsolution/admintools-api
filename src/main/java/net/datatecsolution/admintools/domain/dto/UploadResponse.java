package net.datatecsolution.admintools.domain.dto;

/**
 * Respuesta del endpoint POST /upload. Devuelve las URLs publicas de
 * cada uno de los 3 tamanios generados por UploadService.
 *
 * Las URLs son relativas al servlet root (ej. /uploads/abc-medium.jpg);
 * el cliente debe componerlas con el host del API si necesita una URL
 * absoluta. Esto evita acoplar el storage a un host especifico y
 * facilita el cambio futuro a S3/CDN sin tocar contratos.
 */
public record UploadResponse(
        String urlFull,
        String urlMedium,
        String urlThumb
) {
}

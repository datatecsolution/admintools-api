package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.UploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * US-030 — Upload de imagenes. Recibe MultipartFile, valida (tipo + tamano),
 * redimensiona a 3 tamanios via ImageIO del JDK (sin libs externas) y
 * persiste en disco bajo {@code app.upload.dir}.
 *
 * Los archivos persistidos se exponen publicamente bajo el path
 * {@code /uploads/**} (ver WebMvcConfig.addResourceHandlers).
 *
 * Tamanios generados:
 *   - full   : preserva dimensiones originales (limitado al max del cliente).
 *   - medium : 600x600 maximo, preserva aspect ratio.
 *   - thumb  : 200x200 maximo, preserva aspect ratio.
 *
 * Limitaciones aceptadas:
 *   - Storage local (no S3). Migrar a object storage es US futura.
 *   - Sin EXIF stripping (no esencial para inventario).
 *   - JPEG fija calidad 0.85 al recomprimir (ImageIO default).
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final int MEDIUM_MAX = 600;
    private static final int THUMB_MAX  = 200;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public UploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo es requerido");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo no soportado. Acepta: " + ALLOWED_TYPES);
        }

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se pudo decodificar la imagen");
            }

            String uuid = UUID.randomUUID().toString();
            String ext = extensionFromContentType(contentType);

            String nameFull   = uuid + "-full."   + ext;
            String nameMedium = uuid + "-medium." + ext;
            String nameThumb  = uuid + "-thumb."  + ext;

            writeImage(original, dir.resolve(nameFull), ext);
            writeImage(resize(original, MEDIUM_MAX), dir.resolve(nameMedium), ext);
            writeImage(resize(original, THUMB_MAX),  dir.resolve(nameThumb),  ext);

            log.info("Upload OK uuid={} size_original={}x{} bytes={}",
                    uuid, original.getWidth(), original.getHeight(), file.getSize());

            return new UploadResponse(
                    "/uploads/" + nameFull,
                    "/uploads/" + nameMedium,
                    "/uploads/" + nameThumb);

        } catch (IOException e) {
            log.error("Upload fallo: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al procesar la imagen: " + e.getMessage());
        }
    }

    /** Resize preservando aspect ratio, lado mayor = maxSide. */
    private BufferedImage resize(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSide && h <= maxSide) return src;

        double scale = (double) maxSide / Math.max(w, h);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    private void writeImage(BufferedImage img, Path target, String ext) throws IOException {
        ImageIO.write(img, ext, target.toFile());
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo no soportado");
        };
    }
}

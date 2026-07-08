package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.persistence.crud.ArticuloImagenCRUD;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.entity.ArticuloImagen;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * US-079 — imagen de producto sobre la tabla legacy `articulo_imagen`.
 *
 * Acepta jpeg/png/webp hasta 5MB, redimensiona a lado mayor 600px (misma
 * política que UploadService/US-030) y guarda SIEMPRE recomprimido a JPEG
 * en el blob (una imagen vigente por producto: el store reemplaza).
 * El GET se sirve con caché inmutable — el POS agrega ?v={imageVersion}
 * (el id_img) para romper caché al reemplazarla.
 */
@Service
public class ProductImageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MEDIUM_MAX = 600;
    // Miniatura para el POS (grilla/catálogo/ticket): el POS la renderiza en
    // ≤80px; 160px cubre pantallas retina y pesa ~6× menos que la medium —
    // clave para conexiones lentas (primer render del catálogo).
    private static final int THUMB_MAX = 160;

    private final ArticuloImagenCRUD imagenCRUD;
    private final ArticuloMasterCRUD productCRUD;

    public ProductImageService(ArticuloImagenCRUD imagenCRUD, ArticuloMasterCRUD productCRUD) {
        this.imagenCRUD = imagenCRUD;
        this.productCRUD = productCRUD;
    }

    /** Sube/reemplaza la imagen del producto. Devuelve el id_img nuevo (imageVersion). */
    @Transactional
    public int store(int productId, MultipartFile file) {
        requireProduct(productId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Archivo vacío");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La imagen supera el máximo de 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tipo no soportado. Acepta: " + ALLOWED_TYPES);
        }

        byte[] jpeg;
        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El archivo no es una imagen válida");
            }
            jpeg = toJpegBytes(resize(original, MEDIUM_MAX));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al procesar la imagen: " + e.getMessage(), e);
        }

        // una imagen vigente por producto: reemplaza cualquier fila previa
        imagenCRUD.deleteByCodigoArticulo(productId);
        ArticuloImagen row = new ArticuloImagen();
        row.setCodigoArticulo(productId);
        row.setImg(jpeg);
        row.setExtension("jpg");
        return imagenCRUD.save(row).getIdImg();
    }

    public Optional<ArticuloImagen> get(int productId) {
        return imagenCRUD.findFirstByCodigoArticuloOrderByIdImgDesc(productId);
    }

    /**
     * Miniatura (~160px) generada al vuelo desde el blob medium (600px). No se
     * almacena aparte: el GET la sirve con caché inmutable, así que se genera a
     * lo sumo una vez por terminal. Si el blob no decodifica, devuelve el
     * original (degradación segura). El resize 600→160px es barato (µs-ms).
     */
    public byte[] thumbnail(byte[] mediumJpeg) {
        if (mediumJpeg == null) {
            return null;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(mediumJpeg));
            if (src == null) {
                return mediumJpeg;
            }
            return toJpegBytes(resize(src, THUMB_MAX));
        } catch (IOException e) {
            return mediumJpeg;
        }
    }

    @Transactional
    public void delete(int productId) {
        requireProduct(productId);
        imagenCRUD.deleteByCodigoArticulo(productId);
    }

    private void requireProduct(int productId) {
        if (!productCRUD.existsById(productId)) {
            throw new EntityNotFoundException("Product " + productId + " not found");
        }
    }

    /**
     * Resize preservando aspect ratio, lado mayor = maxSide — misma política
     * que UploadService (US-030). Canvas RGB con fondo blanco para que un
     * PNG/WebP con transparencia no quede sobre negro al pasar a JPEG.
     */
    private static BufferedImage resize(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, (double) maxSide / Math.max(w, h));
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newW, newH);
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

    private static byte[] toJpegBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}

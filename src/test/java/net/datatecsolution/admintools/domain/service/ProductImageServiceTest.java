package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.persistence.crud.ArticuloImagenCRUD;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.entity.ArticuloImagen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-079 — imagen de producto sobre articulo_imagen: validaciones (producto
 * inexistente, tipo, tamaño), reemplazo de la fila previa y recompresión a
 * JPEG medium (lado mayor 600px, fondo blanco para transparencias).
 */
@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock private ArticuloImagenCRUD imagenCRUD;
    @Mock private ArticuloMasterCRUD productCRUD;

    private ProductImageService service() {
        lenient().when(productCRUD.existsById(7)).thenReturn(true);
        return new ProductImageService(imagenCRUD, productCRUD);
    }

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void store_productoInexistente_404() {
        ProductImageService svc = service();
        when(productCRUD.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> svc.store(99,
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void store_tipoNoSoportado_422() {
        ProductImageService svc = service();

        assertThatThrownBy(() -> svc.store(7,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void store_superaLimite5MB_422() {
        ProductImageService svc = service();

        assertThatThrownBy(() -> svc.store(7,
                new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[6 * 1024 * 1024])))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void store_reemplazaLaFilaPreviaYRecomprimeAJpegMedium() throws Exception {
        ProductImageService svc = service();
        when(imagenCRUD.save(any(ArticuloImagen.class))).thenAnswer(inv -> {
            ArticuloImagen row = inv.getArgument(0);
            row.setIdImg(41);
            return row;
        });

        int version = svc.store(7,
                new MockMultipartFile("file", "a.png", "image/png", png(1200, 900)));

        assertThat(version).isEqualTo(41);
        verify(imagenCRUD).deleteByCodigoArticulo(7);

        ArgumentCaptor<ArticuloImagen> captor = ArgumentCaptor.forClass(ArticuloImagen.class);
        verify(imagenCRUD).save(captor.capture());
        ArticuloImagen saved = captor.getValue();
        assertThat(saved.getCodigoArticulo()).isEqualTo(7);
        assertThat(saved.getExtension()).isEqualTo("jpg");
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(saved.getImg()));
        assertThat(stored.getWidth()).isEqualTo(600);   // lado mayor clampeado
        assertThat(stored.getHeight()).isEqualTo(450);  // aspect ratio preservado
    }

    @Test
    void thumbnail_recomprimeA160pxYMasChico() throws Exception {
        ProductImageService svc = service();
        byte[] medium = png(600, 400);

        byte[] thumb = svc.thumbnail(medium);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(thumb));
        assertThat(out.getWidth()).isEqualTo(160);   // lado mayor clampeado
        assertThat(out.getHeight()).isEqualTo(107);  // aspect ratio preservado
        assertThat(thumb.length).isLessThan(medium.length);
    }

    @Test
    void thumbnail_blobIlegible_devuelveElOriginal() {
        ProductImageService svc = service();
        byte[] noEsImagen = {1, 2, 3, 4};

        assertThat(svc.thumbnail(noEsImagen)).isEqualTo(noEsImagen);
    }

    @Test
    void delete_borraLasFilasDelProducto() {
        ProductImageService svc = service();

        svc.delete(7);

        verify(imagenCRUD).deleteByCodigoArticulo(7);
    }
}

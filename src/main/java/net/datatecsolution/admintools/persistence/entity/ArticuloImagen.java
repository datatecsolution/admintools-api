package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * US-079 — imagen del producto EN la base de datos.
 *
 * Reusa la tabla legacy `articulo_imagen` (está en la baseline V1 de la común
 * y verificada en clientes; nadie la usaba). Decisión de negocio 2026-07-05:
 * priorizar simpleza — el blob vive en la BD, entra al mysqldump del cliente
 * y sobrevive redeploys Docker sin volúmenes. Se guarda SOLO el tamaño medium
 * recomprimido a JPEG (~100-200KB); el blob NUNCA viaja en los listados
 * (se sirve por GET /products/{id}/image con caché inmutable).
 */
@Entity
@Table(name = "articulo_imagen")
public class ArticuloImagen {

    @Id
    @Column(name = "id_img")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idImg;

    @Column(name = "codigo_articulo")
    private Integer codigoArticulo;

    @Column(name = "img", columnDefinition = "mediumblob")
    private byte[] img;

    @Column(name = "extension")
    private String extension;

    public Integer getIdImg() {
        return idImg;
    }

    public void setIdImg(Integer idImg) {
        this.idImg = idImg;
    }

    public Integer getCodigoArticulo() {
        return codigoArticulo;
    }

    public void setCodigoArticulo(Integer codigoArticulo) {
        this.codigoArticulo = codigoArticulo;
    }

    public byte[] getImg() {
        return img;
    }

    public void setImg(byte[] img) {
        this.img = img;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}

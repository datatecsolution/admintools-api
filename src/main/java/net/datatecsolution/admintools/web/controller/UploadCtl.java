package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.UploadResponse;
import net.datatecsolution.admintools.domain.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-030 — Upload de imagenes. Solo ADMIN puede subir; las imagenes
 * resultantes se sirven publicamente bajo {@code /uploads/**} sin
 * necesidad de autenticacion (configurado en WebMvcConfig).
 *
 * El cuerpo de la logica esta en {@link UploadService}: valida, redimensiona
 * y persiste; este controller solo orquesta el contrato HTTP.
 */
@RestController
@RequestMapping("/upload")
@Tag(name = "Upload", description = "Upload de imagenes (US-030)")
public class UploadCtl {

    private final UploadService service;

    public UploadCtl(UploadService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir imagen y obtener URLs de 3 tamanios (full/medium/thumb)")
    public ResponseEntity<UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.store(file));
    }
}

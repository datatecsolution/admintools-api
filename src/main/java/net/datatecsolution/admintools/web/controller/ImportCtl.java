package net.datatecsolution.admintools.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.datatecsolution.admintools.domain.dto.ImportReport;
import net.datatecsolution.admintools.domain.service.importer.CustomerImportService;
import net.datatecsolution.admintools.domain.service.importer.ProductImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * US-043/044 — import masivo de productos y clientes desde CSV/.xlsx.
 *
 * Flujo del POS: descargar plantilla → subir con dryRun=true (reporte de
 * validación, no persiste) → si 0 errores, repetir con dryRun=false.
 * El import real es todo-o-nada: con errores responde 400 con el reporte
 * y NO importa nada (rollback del DoD).
 */
@RestController
@Tag(name = "Imports", description = "Import masivo de productos y clientes (US-043/044)")
public class ImportCtl {

    private final ProductImportService productImportService;
    private final CustomerImportService customerImportService;

    public ImportCtl(ProductImportService productImportService,
                     CustomerImportService customerImportService) {
        this.productImportService = productImportService;
        this.customerImportService = customerImportService;
    }

    @GetMapping(value = "/products/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Plantilla CSV de import de productos (US-043)")
    public ResponseEntity<byte[]> productTemplate() {
        return csv("plantilla_productos.csv", productImportService.template());
    }

    @PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import masivo de productos CSV/.xlsx, máx 5000 filas (US-043)")
    public ResponseEntity<ImportReport> importProducts(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun) {
        return reportResponse(productImportService.importFile(file, dryRun), dryRun);
    }

    @GetMapping(value = "/customers/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Plantilla CSV de import de clientes (US-044)")
    public ResponseEntity<byte[]> customerTemplate() {
        return csv("plantilla_clientes.csv", customerImportService.template());
    }

    @PostMapping(value = "/customers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Import masivo de clientes CSV/.xlsx, máx 2000 filas (US-044)")
    public ResponseEntity<ImportReport> importCustomers(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
            Principal principal) {
        return reportResponse(
                customerImportService.importFile(file, dryRun, principal.getName()), dryRun);
    }

    /** dryRun siempre 200; import real con errores → 400 (no se importó nada). */
    private ResponseEntity<ImportReport> reportResponse(ImportReport report, boolean dryRun) {
        if (!dryRun && !report.errors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(report);
        }
        return ResponseEntity.ok(report);
    }

    /** CSV con BOM para que Excel abra los acentos bien de un doble-clic. */
    private ResponseEntity<byte[]> csv(String filename, String content) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(out);
    }
}

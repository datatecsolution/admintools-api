package net.datatecsolution.admintools.domain.service.importer;

import net.datatecsolution.admintools.domain.dto.ImportError;
import net.datatecsolution.admintools.domain.dto.ImportReport;
import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.service.ProductMasterService;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedFile;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedRow;
import net.datatecsolution.admintools.persistence.crud.CategoriaCRUD;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * US-043 — import masivo de productos desde CSV/.xlsx (máx 5000 filas).
 *
 * Columnas (header fila 1): nombre* ; precio* ; categoria* (nombre exacto,
 * debe existir) ; impuesto* (15 | 18 | 0 | EXENTO) ; codigo (alterno,
 * opcional) ; codigos_barra (opcional, varios separados por |) ; activo
 * (SI/NO, default SI).
 *
 * Validación previa completa (dry-run) con reporte por fila/columna.
 * El import real es TODO-O-NADA: solo corre si el archivo entero es válido
 * (rollback del DoD); reutiliza ProductMasterService.create para respetar
 * las invariantes de barcodes (unicidad global 409) y el shape del master.
 *
 * Nota de alcance: el precio va al fallback legacy articulo.precio_articulo
 * (el POS lo muestra como precio público cuando no hay precios_articulos);
 * los precios por tipo se asignan después desde la pantalla de productos.
 */
@Service
public class ProductImportService {

    public static final int MAX_ROWS = 5000;

    private static final List<String> REQUIRED_COLUMNS =
            List.of("nombre", "precio", "categoria", "impuesto");

    private static final String TEMPLATE =
            "nombre;precio;categoria;impuesto;codigo;codigos_barra;activo\n"
            + "Coca Cola 600ml;25.00;OTROS;15;0;7421000000001|7421000000002;SI\n"
            + "Servicio de envio;50.00;OTROS;EXENTO;;;SI\n";

    private final TabularFileParser parser;
    private final ProductMasterService productMasterService;
    private final CategoriaCRUD categoriaCRUD;
    private final JdbcTemplate jdbc;

    public ProductImportService(TabularFileParser parser,
                                ProductMasterService productMasterService,
                                CategoriaCRUD categoriaCRUD,
                                JdbcTemplate jdbc) {
        this.parser = parser;
        this.productMasterService = productMasterService;
        this.categoriaCRUD = categoriaCRUD;
        this.jdbc = jdbc;
    }

    /** Plantilla CSV descargable (delimitador ';', abre directo en Excel). */
    public String template() {
        return TEMPLATE;
    }

    private record Candidate(int rowNumber, ProductRequest request,
                             String nombreLower, List<String> barcodes) {
    }

    @Transactional
    public ImportReport importFile(MultipartFile file, boolean dryRun) {
        ParsedFile parsed = parser.parse(file, MAX_ROWS);
        List<ImportError> errors = new ArrayList<>();
        Set<Integer> filasConError = new HashSet<>();

        for (String col : REQUIRED_COLUMNS) {
            if (!parsed.headers().contains(col)) {
                errors.add(new ImportError(1, col,
                        "Falta la columna obligatoria '" + col + "' en el encabezado"));
            }
        }
        if (!errors.isEmpty()) {
            return new ImportReport(parsed.rows().size(), 0, 0, dryRun, errors);
        }

        // catálogos: categoría por nombre (ambigüedad = error) e impuesto por tasa
        Map<String, Integer> categorias = new HashMap<>();
        Set<String> categoriasAmbiguas = new HashSet<>();
        categoriaCRUD.findAll().forEach(c -> {
            String key = c.getDescripcion().trim().toLowerCase(Locale.ROOT);
            if (categorias.putIfAbsent(key, c.getId()) != null) {
                categoriasAmbiguas.add(key);
            }
        });
        Map<String, Integer> impuestos = new HashMap<>(); // "0"/"15"/"18" -> codigo
        jdbc.query("SELECT codigo_impuesto, porcentaje FROM impuesto", rs -> {
            impuestos.put(String.valueOf(rs.getInt("porcentaje")), rs.getInt("codigo_impuesto"));
        });

        // pasada 1: validación por fila
        List<Candidate> candidates = new ArrayList<>();
        Set<String> nombresEnArchivo = new HashSet<>();
        Map<String, Integer> barcodesEnArchivo = new HashMap<>(); // barcode -> primera fila
        for (ParsedRow row : parsed.rows()) {
            int n = row.rowNumber();
            boolean ok = true;

            String nombre = row.get("nombre");
            if (nombre.isEmpty()) {
                errors.add(new ImportError(n, "nombre", "El nombre es obligatorio"));
                ok = false;
            } else if (nombre.length() > 255) {
                errors.add(new ImportError(n, "nombre", "El nombre excede 255 caracteres"));
                ok = false;
            } else if (!nombresEnArchivo.add(nombre.toLowerCase(Locale.ROOT))) {
                errors.add(new ImportError(n, "nombre",
                        "Nombre duplicado dentro del archivo: '" + nombre + "'"));
                ok = false;
            }

            BigDecimal precio = null;
            String precioRaw = row.get("precio");
            if (precioRaw.isEmpty()) {
                errors.add(new ImportError(n, "precio", "El precio es obligatorio"));
                ok = false;
            } else {
                try {
                    precio = new BigDecimal(normalizeDecimal(precioRaw));
                    if (precio.signum() < 0) {
                        errors.add(new ImportError(n, "precio", "El precio no puede ser negativo"));
                        ok = false;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new ImportError(n, "precio",
                            "Precio inválido: '" + precioRaw + "'"));
                    ok = false;
                }
            }

            Integer categoriaId = null;
            String categoria = row.get("categoria").toLowerCase(Locale.ROOT);
            if (categoria.isEmpty()) {
                errors.add(new ImportError(n, "categoria", "La categoría es obligatoria"));
                ok = false;
            } else if (categoriasAmbiguas.contains(categoria)) {
                errors.add(new ImportError(n, "categoria",
                        "Categoría ambigua (hay más de una con ese nombre): '" + row.get("categoria") + "'"));
                ok = false;
            } else {
                categoriaId = categorias.get(categoria);
                if (categoriaId == null) {
                    errors.add(new ImportError(n, "categoria",
                            "La categoría no existe: '" + row.get("categoria") + "'. Créela antes de importar."));
                    ok = false;
                }
            }

            Integer taxId = null;
            String impuesto = row.get("impuesto").toUpperCase(Locale.ROOT).replace("%", "").trim();
            if (impuesto.isEmpty()) {
                errors.add(new ImportError(n, "impuesto", "El impuesto es obligatorio (15, 18, 0 o EXENTO)"));
                ok = false;
            } else {
                if (impuesto.equals("EXENTO")) {
                    impuesto = "0";
                }
                taxId = impuestos.get(impuesto);
                if (taxId == null) {
                    errors.add(new ImportError(n, "impuesto",
                            "Impuesto inválido: '" + row.get("impuesto") + "' (use 15, 18, 0 o EXENTO)"));
                    ok = false;
                }
            }

            int altCode = 0;
            String codigoRaw = row.get("codigo");
            if (!codigoRaw.isEmpty()) {
                try {
                    altCode = Integer.parseInt(codigoRaw);
                    if (altCode < 0) {
                        errors.add(new ImportError(n, "codigo", "El código debe ser >= 0"));
                        ok = false;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new ImportError(n, "codigo",
                            "Código inválido (debe ser numérico): '" + codigoRaw + "'"));
                    ok = false;
                }
            }

            List<String> barcodes = new ArrayList<>();
            String barcodesRaw = row.get("codigos_barra");
            if (!barcodesRaw.isEmpty()) {
                for (String b : barcodesRaw.split("\\|")) {
                    String code = b.trim();
                    if (code.isEmpty()) {
                        continue;
                    }
                    Integer filaPrevia = barcodesEnArchivo.putIfAbsent(code, n);
                    if (filaPrevia != null && filaPrevia != n) {
                        errors.add(new ImportError(n, "codigos_barra",
                                "Código de barra duplicado dentro del archivo (fila " + filaPrevia + "): " + code));
                        ok = false;
                    } else if (!barcodes.contains(code)) {
                        barcodes.add(code);
                    }
                }
            }

            Boolean activo = parseBoolean(row.get("activo"), true);
            if (activo == null) {
                errors.add(new ImportError(n, "activo",
                        "Valor inválido: '" + row.get("activo") + "' (use SI o NO)"));
                ok = false;
            }

            if (ok) {
                candidates.add(new Candidate(n,
                        new ProductRequest(nombre, precio, categoriaId, taxId,
                                altCode, 1, activo, barcodes, null),
                        nombre.toLowerCase(Locale.ROOT), barcodes));
            } else {
                filasConError.add(n);
            }
        }

        // pasada 2 (batch): duplicados contra la BD
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbc);

        Set<String> nombresExistentes = new HashSet<>();
        List<String> nombres = candidates.stream().map(Candidate::nombreLower).toList();
        for (List<String> chunk : chunks(nombres, 1000)) {
            nombresExistentes.addAll(named.queryForList(
                    "SELECT LOWER(articulo) FROM articulo WHERE LOWER(articulo) IN (:xs)",
                    Map.of("xs", chunk), String.class));
        }
        Set<String> barcodesExistentes = new HashSet<>();
        List<String> allBarcodes = candidates.stream()
                .flatMap(c -> c.barcodes().stream()).toList();
        for (List<String> chunk : chunks(allBarcodes, 1000)) {
            barcodesExistentes.addAll(named.queryForList(
                    "SELECT codigo_barra FROM codigos_articulos WHERE codigo_barra IN (:xs)",
                    Map.of("xs", chunk), String.class));
        }
        for (Candidate c : candidates) {
            if (nombresExistentes.contains(c.nombreLower())) {
                errors.add(new ImportError(c.rowNumber(), "nombre",
                        "Ya existe un producto con ese nombre: '" + c.request().name()
                        + "' (los duplicados generan stock fantasma)"));
                filasConError.add(c.rowNumber());
            }
            for (String b : c.barcodes()) {
                if (barcodesExistentes.contains(b)) {
                    errors.add(new ImportError(c.rowNumber(), "codigos_barra",
                            "El código de barra ya pertenece a otro producto: " + b));
                    filasConError.add(c.rowNumber());
                }
            }
        }

        List<Candidate> validos = candidates.stream()
                .filter(c -> !filasConError.contains(c.rowNumber())).toList();
        int total = parsed.rows().size();

        if (dryRun || !errors.isEmpty()) {
            // con errores NUNCA se importa (todo-o-nada); el controller decide el status
            return new ImportReport(total, validos.size(), 0, dryRun, errors);
        }
        for (Candidate c : validos) {
            productMasterService.create(c.request());
        }
        return new ImportReport(total, validos.size(), validos.size(), false, List.of());
    }

    /** "25,50" → "25.50" (solo si no hay punto; los miles no se soportan). */
    private static String normalizeDecimal(String raw) {
        return raw.contains(".") ? raw : raw.replace(',', '.');
    }

    /** SI/NO/1/0/TRUE/FALSE/vacío(default). null = inválido. */
    private static Boolean parseBoolean(String raw, boolean def) {
        if (raw == null || raw.isBlank()) {
            return def;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "SI", "SÍ", "1", "TRUE", "S" -> Boolean.TRUE;
            case "NO", "0", "FALSE", "N" -> Boolean.FALSE;
            default -> null;
        };
    }

    static <T> List<List<T>> chunks(List<T> xs, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < xs.size(); i += size) {
            out.add(xs.subList(i, Math.min(i + size, xs.size())));
        }
        return out;
    }
}

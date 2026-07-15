package net.datatecsolution.admintools.domain.service.importer;

import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.ImportError;
import net.datatecsolution.admintools.domain.dto.ImportReport;
import net.datatecsolution.admintools.domain.service.CustomerService;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedFile;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedRow;
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
 * US-044 — import masivo de clientes desde CSV/.xlsx (máx 2000 filas).
 *
 * Columnas (header fila 1): nombre* ; rtn ('CF' o 14 dígitos; vacío = CF) ;
 * tipo (CONTADO/CREDITO, default CONTADO) ; telefono ; celular ; direccion ;
 * limite_credito (obligatorio > 0 si tipo=CREDITO).
 *
 * Mismas reglas de negocio que el alta manual: reutiliza
 * CustomerService.create (import es ADMIN-only → isAdmin=true; el vendedor
 * asignado se resuelve del usuario autenticado, igual que el alta manual).
 * Todo-o-nada: con errores no se importa nada.
 */
@Service
public class CustomerImportService {

    public static final int MAX_ROWS = 2000;

    private static final List<String> REQUIRED_COLUMNS = List.of("nombre");

    private static final String TEMPLATE =
            "nombre;rtn;tipo;telefono;celular;direccion;limite_credito\n"
            + "Juan Perez;08011999123456;CREDITO;2222-3333;9999-8888;Col. Centro, Tegucigalpa;5000\n"
            + "Cliente de mostrador;CF;CONTADO;;;;\n";

    private final TabularFileParser parser;
    private final CustomerService customerService;
    private final JdbcTemplate jdbc;

    public CustomerImportService(TabularFileParser parser,
                                 CustomerService customerService,
                                 JdbcTemplate jdbc) {
        this.parser = parser;
        this.customerService = customerService;
        this.jdbc = jdbc;
    }

    public String template() {
        return TEMPLATE;
    }

    private record Candidate(int rowNumber, CustomerCreateRequest request,
                             String nombreLower, String rtn) {
    }

    @Transactional
    public ImportReport importFile(MultipartFile file, boolean dryRun, String user) {
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

        List<Candidate> candidates = new ArrayList<>();
        Set<String> nombresEnArchivo = new HashSet<>();
        Map<String, Integer> rtnEnArchivo = new HashMap<>(); // rtn real (≠CF) -> primera fila
        for (ParsedRow row : parsed.rows()) {
            int n = row.rowNumber();
            boolean ok = true;

            String nombre = row.get("nombre");
            if (nombre.isEmpty()) {
                errors.add(new ImportError(n, "nombre", "El nombre es obligatorio"));
                ok = false;
            } else if (!nombresEnArchivo.add(nombre.toLowerCase(Locale.ROOT))) {
                errors.add(new ImportError(n, "nombre",
                        "Nombre duplicado dentro del archivo: '" + nombre + "'"));
                ok = false;
            }

            String rtn = row.get("rtn").toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
            if (rtn.isEmpty()) {
                rtn = "CF";
            }
            if (!rtn.equals("CF") && !rtn.matches("\\d{14}")) {
                errors.add(new ImportError(n, "rtn",
                        "RTN inválido: '" + row.get("rtn") + "' (debe ser 'CF' o 14 dígitos)"));
                ok = false;
            } else if (!rtn.equals("CF")) {
                Integer filaPrevia = rtnEnArchivo.putIfAbsent(rtn, n);
                if (filaPrevia != null) {
                    errors.add(new ImportError(n, "rtn",
                            "RTN duplicado dentro del archivo (fila " + filaPrevia + "): " + rtn));
                    ok = false;
                }
            }

            String tipoRaw = row.get("tipo").toUpperCase(Locale.ROOT);
            Integer tipo;
            switch (tipoRaw) {
                case "", "CONTADO", "1" -> tipo = 1;
                case "CREDITO", "CRÉDITO", "2" -> tipo = 2;
                default -> {
                    errors.add(new ImportError(n, "tipo",
                            "Tipo inválido: '" + row.get("tipo") + "' (use CONTADO o CREDITO)"));
                    tipo = null;
                    ok = false;
                }
            }

            String telefono = emptyToNull(row.get("telefono"));
            String celular = emptyToNull(row.get("celular"));
            String direccion = emptyToNull(row.get("direccion"));
            if (telefono != null && telefono.length() > 20) {
                errors.add(new ImportError(n, "telefono", "El teléfono excede 20 caracteres"));
                ok = false;
            }
            if (celular != null && celular.length() > 20) {
                errors.add(new ImportError(n, "celular", "El celular excede 20 caracteres"));
                ok = false;
            }
            if (direccion != null && direccion.length() > 255) {
                errors.add(new ImportError(n, "direccion", "La dirección excede 255 caracteres"));
                ok = false;
            }

            BigDecimal limite = null;
            String limiteRaw = row.get("limite_credito");
            if (!limiteRaw.isEmpty()) {
                try {
                    limite = new BigDecimal(limiteRaw.contains(".") ? limiteRaw : limiteRaw.replace(',', '.'));
                    if (limite.signum() < 0) {
                        errors.add(new ImportError(n, "limite_credito", "El límite no puede ser negativo"));
                        ok = false;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new ImportError(n, "limite_credito",
                            "Límite inválido: '" + limiteRaw + "'"));
                    ok = false;
                }
            }
            // misma regla que el alta manual de crédito (CustomerService.create)
            if (tipo != null && tipo == 2) {
                if (telefono == null || direccion == null
                        || limite == null || limite.signum() <= 0) {
                    errors.add(new ImportError(n, "tipo",
                            "El cliente de CREDITO requiere teléfono, dirección y límite de crédito > 0"));
                    ok = false;
                }
            }

            if (ok) {
                candidates.add(new Candidate(n,
                        new CustomerCreateRequest(nombre, rtn, direccion, telefono,
                                celular, tipo, limite, null),
                        nombre.toLowerCase(Locale.ROOT), rtn));
            } else {
                filasConError.add(n);
            }
        }

        // batch contra la BD: nombre exacto y RTN real ya registrados
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbc);
        Set<String> nombresExistentes = new HashSet<>();
        List<String> nombres = candidates.stream().map(Candidate::nombreLower).toList();
        for (List<String> chunk : ProductImportService.chunks(nombres, 1000)) {
            nombresExistentes.addAll(named.queryForList(
                    "SELECT LOWER(nombre_cliente) FROM cliente WHERE LOWER(nombre_cliente) IN (:xs)",
                    Map.of("xs", chunk), String.class));
        }
        Set<String> rtnExistentes = new HashSet<>();
        List<String> rtns = candidates.stream().map(Candidate::rtn)
                .filter(r -> !r.equals("CF")).toList();
        for (List<String> chunk : ProductImportService.chunks(rtns, 1000)) {
            rtnExistentes.addAll(named.queryForList(
                    "SELECT rtn FROM cliente WHERE rtn IN (:xs)",
                    Map.of("xs", chunk), String.class));
        }
        for (Candidate c : candidates) {
            if (nombresExistentes.contains(c.nombreLower())) {
                errors.add(new ImportError(c.rowNumber(), "nombre",
                        "Ya existe un cliente con ese nombre: '" + c.request().name() + "'"));
                filasConError.add(c.rowNumber());
            }
            if (!c.rtn().equals("CF") && rtnExistentes.contains(c.rtn())) {
                errors.add(new ImportError(c.rowNumber(), "rtn",
                        "Ya existe un cliente con ese RTN: " + c.rtn()));
                filasConError.add(c.rowNumber());
            }
        }

        List<Candidate> validos = candidates.stream()
                .filter(c -> !filasConError.contains(c.rowNumber())).toList();
        int total = parsed.rows().size();

        if (dryRun || !errors.isEmpty()) {
            return new ImportReport(total, validos.size(), 0, dryRun, errors);
        }
        for (Candidate c : validos) {
            customerService.create(c.request(), user, true);
        }
        return new ImportReport(total, validos.size(), validos.size(), false, List.of());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}

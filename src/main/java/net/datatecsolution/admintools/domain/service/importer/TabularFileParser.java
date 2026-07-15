package net.datatecsolution.admintools.domain.service.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * US-043/044 — parser común de archivos tabulares de import.
 * Acepta .csv (UTF-8, delimitador ',' o ';' autodetectado, BOM tolerado)
 * y .xlsx (primera hoja, POI). La primera fila es el header; cada fila de
 * datos se devuelve como mapa header-normalizado (lowercase, trim) → valor.
 *
 * El número de fila reportado es el de Excel/editor (header = fila 1).
 */
@Component
public class TabularFileParser {

    /** Fila parseada: número visible (1-based con header=1) + valores por columna. */
    public record ParsedRow(int rowNumber, Map<String, String> values) {

        /** Valor de la columna (header normalizado), trim, o "" si no está. */
        public String get(String column) {
            String v = values.get(column);
            return v == null ? "" : v.trim();
        }
    }

    /** Resultado del parseo: headers normalizados + filas de datos no vacías. */
    public record ParsedFile(List<String> headers, List<ParsedRow> rows) {
    }

    /**
     * @param maxRows máximo de filas de DATOS permitidas.
     * @throws IllegalArgumentException archivo ilegible, extensión no
     *         soportada o límite de filas excedido (→ 400 vía handler).
     */
    public ParsedFile parse(MultipartFile file, int maxRows) {
        String name = file.getOriginalFilename() == null ? "" :
                file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            ParsedFile parsed;
            if (name.endsWith(".xlsx")) {
                parsed = parseXlsx(bytes);
            } else if (name.endsWith(".csv") || name.endsWith(".txt")) {
                parsed = parseCsv(bytes);
            } else {
                throw new IllegalArgumentException(
                        "Formato no soportado: use .csv o .xlsx (recibido: " + name + ")");
            }
            if (parsed.rows().size() > maxRows) {
                throw new IllegalArgumentException("El archivo tiene "
                        + parsed.rows().size() + " filas de datos; el máximo es " + maxRows);
            }
            return parsed;
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private ParsedFile parseCsv(byte[] bytes) throws IOException {
        // strip BOM (Excel exporta "CSV UTF-8" con BOM)
        int offset = (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) ? 3 : 0;
        char delimiter = detectDelimiter(bytes, offset);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(bytes, offset, bytes.length - offset),
                        StandardCharsets.UTF_8), format)) {
            List<String> headers = parser.getHeaderNames().stream()
                    .map(h -> h.trim().toLowerCase(Locale.ROOT))
                    .toList();
            List<ParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < record.size(); i++) {
                    values.put(headers.get(i), record.get(i));
                }
                if (values.values().stream().allMatch(v -> v == null || v.isBlank())) {
                    continue; // fila totalmente vacía
                }
                // getRecordNumber() es 1-based sobre las filas de datos → +1 por el header
                rows.add(new ParsedRow((int) record.getRecordNumber() + 1, values));
            }
            return new ParsedFile(headers, rows);
        }
    }

    /** Heurística: gana el separador más frecuente en la línea del header. */
    private char detectDelimiter(byte[] bytes, int offset) {
        int commas = 0;
        int semicolons = 0;
        for (int i = offset; i < bytes.length; i++) {
            char c = (char) (bytes[i] & 0xFF);
            if (c == '\n' || c == '\r') {
                break;
            }
            if (c == ',') {
                commas++;
            } else if (c == ';') {
                semicolons++;
            }
        }
        return semicolons >= commas ? ';' : ',';
    }

    private ParsedFile parseXlsx(byte[] bytes) throws IOException {
        DataFormatter fmt = new DataFormatter();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("La hoja está vacía (sin fila de encabezados)");
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(fmt.formatCellValue(headerRow.getCell(c))
                        .trim().toLowerCase(Locale.ROOT));
            }
            List<ParsedRow> rows = new ArrayList<>();
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String v = fmt.formatCellValue(row.getCell(c)).trim();
                    if (!v.isEmpty()) {
                        empty = false;
                    }
                    values.put(headers.get(c), v);
                }
                if (!empty) {
                    rows.add(new ParsedRow(r + 1, values)); // POI es 0-based → +1
                }
            }
            return new ParsedFile(headers, rows);
        }
    }
}

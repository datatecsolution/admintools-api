package net.datatecsolution.admintools.domain.service.importer;

import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US-043/044 — parser común de CSV/xlsx. Cubre autodetección de delimitador
 * (',' vs ';'), BOM tolerado, salteo de filas vacías, numeración estilo Excel
 * (header = fila 1) y el tope de filas. El xlsx queda como happy-path (POI).
 */
class TabularFileParserTest {

    private final TabularFileParser parser = new TabularFileParser();

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "productos.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parseCsv_delimitadorComa_leeHeadersYFilas() {
        ParsedFile pf = parser.parse(csv("nombre,precio\nCoca Cola,25.00\nPepsi,30.00\n"), 5000);

        assertThat(pf.headers()).containsExactly("nombre", "precio");
        assertThat(pf.rows()).hasSize(2);
        assertThat(pf.rows().get(0).get("nombre")).isEqualTo("Coca Cola");
        assertThat(pf.rows().get(1).get("precio")).isEqualTo("30.00");
    }

    @Test
    void parseCsv_delimitadorPuntoYComa_autodetectado() {
        ParsedFile pf = parser.parse(csv("nombre;precio;categoria\nCoca;25;OTROS\n"), 5000);

        assertThat(pf.headers()).containsExactly("nombre", "precio", "categoria");
        assertThat(pf.rows()).hasSize(1);
        assertThat(pf.rows().get(0).get("categoria")).isEqualTo("OTROS");
    }

    @Test
    void parseCsv_headerConMayusculasYEspacios_seNormaliza() {
        ParsedFile pf = parser.parse(csv(" Nombre , PRECIO \nCoca,25\n"), 5000);

        assertThat(pf.headers()).containsExactly("nombre", "precio");
    }

    @Test
    void parseCsv_conBOM_noContaminaElPrimerHeader() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "nombre,precio\nCoca,25\n".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);
        MockMultipartFile file = new MockMultipartFile("file", "p.csv", "text/csv", withBom);

        ParsedFile pf = parser.parse(file, 5000);

        assertThat(pf.headers()).containsExactly("nombre", "precio");
        assertThat(pf.rows().get(0).get("nombre")).isEqualTo("Coca");
    }

    @Test
    void parseCsv_filasVaciasSeSaltean() {
        ParsedFile pf = parser.parse(csv("nombre,precio\nCoca,25\n , \nPepsi,30\n"), 5000);

        assertThat(pf.rows()).hasSize(2);
        assertThat(pf.rows().get(1).get("nombre")).isEqualTo("Pepsi");
    }

    @Test
    void parseCsv_numeracionEstiloExcel_headerEs1YPrimerDatoEs2() {
        ParsedFile pf = parser.parse(csv("nombre,precio\nCoca,25\nPepsi,30\n"), 5000);

        assertThat(pf.rows().get(0).rowNumber()).isEqualTo(2);
        assertThat(pf.rows().get(1).rowNumber()).isEqualTo(3);
    }

    @Test
    void parse_excedeMaxRows_lanzaIllegalArgument() {
        assertThatThrownBy(() -> parser.parse(csv("nombre,precio\nCoca,25\nPepsi,30\n"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo");
    }

    @Test
    void parse_extensionNoSoportada_lanzaIllegalArgument() {
        MockMultipartFile pdf = new MockMultipartFile("file", "datos.pdf",
                "application/pdf", "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(pdf, 5000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado");
    }

    @Test
    void parseXlsx_happyPath_leeHeadersFilasYNumeracion() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("hoja1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Precio");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("Coca Cola");
            data.createCell(1).setCellValue("25.00");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile("file", "productos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        ParsedFile pf = parser.parse(file, 5000);

        assertThat(pf.headers()).containsExactly("nombre", "precio");
        assertThat(pf.rows()).hasSize(1);
        assertThat(pf.rows().get(0).get("nombre")).isEqualTo("Coca Cola");
        assertThat(pf.rows().get(0).rowNumber()).isEqualTo(2);
    }
}

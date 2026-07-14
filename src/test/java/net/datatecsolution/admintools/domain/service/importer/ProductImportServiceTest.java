package net.datatecsolution.admintools.domain.service.importer;

import net.datatecsolution.admintools.domain.dto.ImportError;
import net.datatecsolution.admintools.domain.dto.ImportReport;
import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.service.ProductMasterService;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedFile;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedRow;
import net.datatecsolution.admintools.persistence.crud.CategoriaCRUD;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.multipart.MultipartFile;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-043 — validación por fila del import de productos (dry-run). El parser y
 * los accesos a BD (catálogo de impuestos, duplicados) se mockean: el foco es
 * el reporte de validación y la regla todo-o-nada (con errores no se crea nada).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductImportServiceTest {

    @Mock private TabularFileParser parser;
    @Mock private ProductMasterService productMasterService;
    @Mock private CategoriaCRUD categoriaCRUD;
    @Mock private JdbcTemplate jdbc;
    @Mock private MultipartFile file;

    private ProductImportService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        service = new ProductImportService(parser, productMasterService, categoriaCRUD, jdbc);

        // catálogo de categorías: solo existe "OTROS" (id 5)
        Categoria otros = new Categoria();
        otros.setId(5);
        otros.setDescripcion("OTROS");
        when(categoriaCRUD.findAll()).thenReturn(List.of(otros));

        // catálogo de impuestos: 15->1, 18->2, 0->3
        doAnswer(inv -> {
            RowCallbackHandler h = inv.getArgument(1);
            h.processRow(impuestoRow(15, 1));
            h.processRow(impuestoRow(18, 2));
            h.processRow(impuestoRow(0, 3));
            return null;
        }).when(jdbc).query(eq("SELECT codigo_impuesto, porcentaje FROM impuesto"),
                any(RowCallbackHandler.class));

        // pasada 2 (duplicados contra la BD): nada existe todavía
        when(jdbc.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
                .thenReturn(List.of());
    }

    private ResultSet impuestoRow(int porcentaje, int codigo) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("porcentaje")).thenReturn(porcentaje);
        when(rs.getInt("codigo_impuesto")).thenReturn(codigo);
        return rs;
    }

    private ParsedRow row(int n, String nombre, String precio, String categoria, String impuesto) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("nombre", nombre);
        v.put("precio", precio);
        v.put("categoria", categoria);
        v.put("impuesto", impuesto);
        return new ParsedRow(n, v);
    }

    private void stubParser(ParsedRow... rows) {
        when(parser.parse(any(), anyInt())).thenReturn(new ParsedFile(
                List.of("nombre", "precio", "categoria", "impuesto"), List.of(rows)));
    }

    @Test
    void dryRun_filaValida_cuentaComoValida() {
        stubParser(row(2, "Coca Cola", "25.00", "OTROS", "15"));

        ImportReport report = service.importFile(file, true);

        assertThat(report.validRows()).isEqualTo(1);
        assertThat(report.importedRows()).isZero();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void dryRun_nombreDuplicadoEnArchivo_generaError() {
        stubParser(
                row(2, "Coca Cola", "25.00", "OTROS", "15"),
                row(3, "Coca Cola", "30.00", "OTROS", "15"));

        ImportReport report = service.importFile(file, true);

        assertThat(report.validRows()).isEqualTo(1);
        assertThat(report.errors()).extracting(ImportError::column).contains("nombre");
        assertThat(report.errors()).anySatisfy(e ->
                assertThat(e.message()).containsIgnoringCase("duplicado"));
    }

    @Test
    void dryRun_precioNoNumerico_generaError() {
        stubParser(row(2, "Coca Cola", "abc", "OTROS", "15"));

        ImportReport report = service.importFile(file, true);

        assertThat(report.validRows()).isZero();
        assertThat(report.errors()).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("precio");
            assertThat(e.row()).isEqualTo(2);
        });
    }

    @Test
    void dryRun_categoriaInexistente_generaError() {
        stubParser(row(2, "Coca Cola", "25.00", "NOEXISTE", "15"));

        ImportReport report = service.importFile(file, true);

        assertThat(report.validRows()).isZero();
        assertThat(report.errors()).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("categoria");
            assertThat(e.message()).containsIgnoringCase("no existe");
        });
    }

    @Test
    void dryRun_impuestoInvalido_generaError() {
        stubParser(row(2, "Coca Cola", "25.00", "OTROS", "99"));

        ImportReport report = service.importFile(file, true);

        assertThat(report.validRows()).isZero();
        assertThat(report.errors()).extracting(ImportError::column).contains("impuesto");
    }

    @Test
    void conErroresYDryRunFalse_noCreaNingunProducto() {
        // una fila válida y una inválida: todo-o-nada, no se crea nada
        stubParser(
                row(2, "Coca Cola", "25.00", "OTROS", "15"),
                row(3, "Fanta", "xx", "OTROS", "15"));

        ImportReport report = service.importFile(file, false);

        assertThat(report.errors()).isNotEmpty();
        assertThat(report.importedRows()).isZero();
        verify(productMasterService, never()).create(any(ProductRequest.class));
    }
}

package net.datatecsolution.admintools.domain.service.importer;

import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.ImportError;
import net.datatecsolution.admintools.domain.dto.ImportReport;
import net.datatecsolution.admintools.domain.service.CustomerService;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedFile;
import net.datatecsolution.admintools.domain.service.importer.TabularFileParser.ParsedRow;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-044 — validación por fila del import de clientes (dry-run). Se mockea el
 * parser y el acceso a BD; el foco es RTN inválido, la regla de crédito
 * (requiere teléfono/dirección/límite) y el todo-o-nada.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerImportServiceTest {

    @Mock private TabularFileParser parser;
    @Mock private CustomerService customerService;
    @Mock private JdbcTemplate jdbc;
    @Mock private MultipartFile file;

    private CustomerImportService service;

    @BeforeEach
    void setUp() {
        service = new CustomerImportService(parser, customerService, jdbc);
        // pasada 2 (duplicados nombre/RTN contra la BD): nada existe todavía
        when(jdbc.query(any(PreparedStatementCreator.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
                .thenReturn(List.of());
    }

    private ParsedRow row(int n, Map<String, String> extra) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("nombre", "");
        v.put("rtn", "");
        v.put("tipo", "");
        v.put("telefono", "");
        v.put("celular", "");
        v.put("direccion", "");
        v.put("limite_credito", "");
        v.putAll(extra);
        return new ParsedRow(n, v);
    }

    private void stubParser(ParsedRow... rows) {
        when(parser.parse(any(), anyInt())).thenReturn(new ParsedFile(
                List.of("nombre", "rtn", "tipo", "telefono", "celular", "direccion", "limite_credito"),
                List.of(rows)));
    }

    @Test
    void dryRun_clienteContadoValido_cuentaComoValido() {
        stubParser(row(2, Map.of("nombre", "Cliente Mostrador", "rtn", "CF", "tipo", "CONTADO")));

        ImportReport report = service.importFile(file, true, "ronal");

        assertThat(report.validRows()).isEqualTo(1);
        assertThat(report.importedRows()).isZero();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void dryRun_rtnInvalido_generaError() {
        // ni 'CF' ni 14 dígitos
        stubParser(row(2, Map.of("nombre", "Juan", "rtn", "123", "tipo", "CONTADO")));

        ImportReport report = service.importFile(file, true, "ronal");

        assertThat(report.validRows()).isZero();
        assertThat(report.errors()).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("rtn");
            assertThat(e.message()).containsIgnoringCase("14 d");
        });
    }

    @Test
    void dryRun_creditoSinLimite_generaError() {
        // CREDITO sin teléfono/dirección/límite viola la regla del alta manual
        stubParser(row(2, Map.of("nombre", "Empresa X", "rtn", "CF", "tipo", "CREDITO")));

        ImportReport report = service.importFile(file, true, "ronal");

        assertThat(report.validRows()).isZero();
        assertThat(report.errors()).anySatisfy(e -> {
            assertThat(e.column()).isEqualTo("tipo");
            assertThat(e.message()).containsIgnoringCase("límite de crédito");
        });
    }

    @Test
    void conErroresYDryRunFalse_noCreaNingunCliente() {
        stubParser(
                row(2, Map.of("nombre", "Cliente Ok", "rtn", "CF", "tipo", "CONTADO")),
                row(3, Map.of("nombre", "Cliente Malo", "rtn", "123", "tipo", "CONTADO")));

        ImportReport report = service.importFile(file, false, "ronal");

        assertThat(report.errors()).isNotEmpty();
        assertThat(report.importedRows()).isZero();
        verify(customerService, never())
                .create(any(CustomerCreateRequest.class), anyString(), anyBoolean());
    }
}

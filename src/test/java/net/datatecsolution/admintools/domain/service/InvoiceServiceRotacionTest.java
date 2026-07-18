package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.InvoiceCreateRequest;
import net.datatecsolution.admintools.domain.dto.InvoiceLineRequest;
import net.datatecsolution.admintools.domain.dto.InvoiceResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigAppCRUD;
import net.datatecsolution.admintools.persistence.crud.ImpuestoCRUD;
import net.datatecsolution.admintools.persistence.crud.OrdenCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.tenant.crud.DatosFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.crud.DetalleFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.crud.EncabezadoFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.entity.EncabezadoFactura;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-102 — integración de la rotación en {@code createDirect}: la decisión se
 * consulta con el flag correcto (consumidor final vs identificado) y, cuando
 * el servicio de rotación devuelve una caja destino, TODO el guardado ocurre
 * con el tenant re-enrutado (se captura {@code TenantContext.getTenant()}
 * dentro del save del encabezado) y la respuesta lo refleja.
 *
 * Lenient: createDirect atraviesa muchos lookups Optional (config, fiscal,
 * QR) que degradan solos con los defaults de Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceRotacionTest {

    private static final Principal TECNICO = () -> "tecnico";
    private static final String DB_DEFAULT = "admin_tools_caja_1";
    private static final String DB_ROTADA = "admin_tools_caja_2";

    @Mock private OrdenCRUD ordenCRUD;
    @Mock private ClienteCRUD clienteCRUD;
    @Mock private EncabezadoFacturaCRUD encabezadoFacturaCRUD;
    @Mock private DetalleFacturaCRUD detalleFacturaCRUD;
    @Mock private CajaCRUD cajaCRUD;
    @Mock private ConfigAppCRUD configAppCRUD;
    @Mock private ImpuestoCRUD impuestoCRUD;
    @Mock private DatosFacturaCRUD datosFacturaCRUD;
    @Mock private AccountsReceivableService accountsReceivableService;
    @Mock private SellerCatalogService sellerCatalogService;
    @Mock private InvoiceQrTokenService qrTokenService;
    @Mock private RotacionCajasService rotacionCajasService;
    @Mock private PlatformTransactionManager commonTm;
    @Mock private PlatformTransactionManager tenantTm;

    private InvoiceService service;
    private final AtomicReference<String> tenantEnElSave = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(DB_DEFAULT);
        service = new InvoiceService(ordenCRUD, clienteCRUD, encabezadoFacturaCRUD,
                detalleFacturaCRUD, cajaCRUD, configAppCRUD, impuestoCRUD,
                datosFacturaCRUD, accountsReceivableService, sellerCatalogService,
                qrTokenService, rotacionCajasService, commonTm, tenantTm);

        // Consumidor final (id 1) y cliente identificado (id 5)
        when(clienteCRUD.findById(1)).thenReturn(Optional.of(cliente(1)));
        when(clienteCRUD.findById(5)).thenReturn(Optional.of(cliente(5)));

        // Catálogo de cajas para codigoCaja/qrCaja
        when(cajaCRUD.findByNombreDb(DB_DEFAULT)).thenReturn(Optional.of(caja(1, DB_DEFAULT)));
        when(cajaCRUD.findByNombreDb(DB_ROTADA)).thenReturn(Optional.of(caja(2, DB_ROTADA)));

        // El save del header captura el tenant VIGENTE en ese momento (es lo
        // que decide a qué BD va el INSERT real) y asigna numeración.
        when(encabezadoFacturaCRUD.save(any(EncabezadoFactura.class))).thenAnswer(inv -> {
            tenantEnElSave.set(TenantContext.getTenant());
            EncabezadoFactura h = inv.getArgument(0);
            h.setNumeroFactura(42);
            return h;
        });
        when(encabezadoFacturaCRUD.findById(42)).thenAnswer(inv ->
                Optional.of(headerGuardado()));
        when(detalleFacturaCRUD.findByNumeroFactura(anyInt())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- helpers ----------

    private static Cliente cliente(int id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setTipoCliente(1);
        return c;
    }

    private static Caja caja(int codigo, String nombreDb) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setNombreDb(nombreDb);
        return c;
    }

    private EncabezadoFactura headerGuardado() {
        EncabezadoFactura h = new EncabezadoFactura();
        h.setNumeroFactura(42);
        h.setTipoFactura(1);
        return h;
    }

    /** Venta de contado con una línea; customerId null = consumidor final. */
    private static InvoiceCreateRequest ventaContado(Integer customerId) {
        return new InvoiceCreateRequest(customerId, null, null, 1, 1, null,
                new BigDecimal("100"), null, null, null,
                List.of(new InvoiceLineRequest(1, BigDecimal.ONE, new BigDecimal("100"),
                        BigDecimal.ZERO, 1)));
    }

    // ---------- casos ----------

    @Test
    void consumidorFinal_consultaRotacionConTrue_ySinRotarQuedaEnDefault() {
        when(rotacionCajasService.decideCaja("tecnico", true)).thenReturn(Optional.empty());

        InvoiceResponse r = service.createDirect(ventaContado(null), TECNICO);

        verify(rotacionCajasService).decideCaja("tecnico", true);
        assertThat(tenantEnElSave.get()).isEqualTo(DB_DEFAULT);
        assertThat(r.tenant()).isEqualTo(DB_DEFAULT);
    }

    @Test
    void clienteIdentificado_consultaRotacionConFalse() {
        when(rotacionCajasService.decideCaja("tecnico", false)).thenReturn(Optional.empty());

        service.createDirect(ventaContado(5), TECNICO);

        verify(rotacionCajasService).decideCaja("tecnico", false);
        assertThat(tenantEnElSave.get()).isEqualTo(DB_DEFAULT);
    }

    @Test
    void ventaRotada_guardaYRespondeConLaCajaDestino() {
        when(rotacionCajasService.decideCaja("tecnico", true))
                .thenReturn(Optional.of(DB_ROTADA));

        InvoiceResponse r = service.createDirect(ventaContado(null), TECNICO);

        // el INSERT del encabezado (y por ende numeración fiscal + trigger
        // kardex) ocurrió con el tenant re-enrutado a la caja 2
        assertThat(tenantEnElSave.get()).isEqualTo(DB_ROTADA);
        assertThat(r.tenant()).isEqualTo(DB_ROTADA);
        assertThat(r.qrCaja()).isEqualTo(2);
    }

    @Test
    void rotacionDevuelveLaMismaDefault_esInocuo() {
        when(rotacionCajasService.decideCaja("tecnico", true))
                .thenReturn(Optional.of(DB_DEFAULT));

        InvoiceResponse r = service.createDirect(ventaContado(null), TECNICO);

        assertThat(tenantEnElSave.get()).isEqualTo(DB_DEFAULT);
        assertThat(r.tenant()).isEqualTo(DB_DEFAULT);
    }
}

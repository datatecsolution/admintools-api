package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.SaleReturnLineRequest;
import net.datatecsolution.admintools.domain.dto.SaleReturnLineResponse;
import net.datatecsolution.admintools.domain.dto.SaleReturnRequest;
import net.datatecsolution.admintools.domain.dto.SaleReturnResponse;
import net.datatecsolution.admintools.persistence.crud.DetalleDevolucionCRUD;
import net.datatecsolution.admintools.persistence.entity.DetalleDevolucion;
import net.datatecsolution.admintools.persistence.tenant.crud.DetalleFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.crud.EncabezadoFacturaCRUD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sale Returns (devoluciones de venta) — el cliente regresa mercaderia
 * que se le habia facturado. Cada item dispara
 * {@code crear_dev_venta_kardex} (con fix V19/V20: header lock + FOR UPDATE
 * saldo + UPSERT a {@code existencia_articulo_bodega}). El stock sube
 * automaticamente; el API NO lo toca.
 *
 * Patron multi-tenant:
 *   - Lee el tenant del JWT (TenantInterceptor de US-017).
 *   - El {@code codigo_caja} se resuelve de {@code admin_tools.cajas}
 *     buscando por {@code nombre_db}. Necesario para el INSERT en
 *     {@code detalle_devoluciones}.
 *   - Las validaciones de existencia de factura y cantidad facturada se
 *     hacen contra la caja del usuario (EncabezadoFacturaCRUD y
 *     DetalleFacturaCRUD viven en el tenant EMF de INV-8).
 *   - El INSERT del documento de devolucion va al common EMF
 *     (admin_tools.detalle_devoluciones).
 */
@Service
public class SaleReturnService {

    private static final Logger log = LoggerFactory.getLogger(SaleReturnService.class);

    private final DetalleDevolucionCRUD devolucionCRUD;
    private final EncabezadoFacturaCRUD encabezadoFacturaCRUD;
    private final DetalleFacturaCRUD detalleFacturaCRUD;
    private final JdbcTemplate commonJdbc;
    private final TransactionTemplate commonTx;
    private final TransactionTemplate tenantTx;

    public SaleReturnService(DetalleDevolucionCRUD devolucionCRUD,
                             EncabezadoFacturaCRUD encabezadoFacturaCRUD,
                             DetalleFacturaCRUD detalleFacturaCRUD,
                             @Qualifier("commonDataSource") DataSource commonDS,
                             @Qualifier("transactionManager") PlatformTransactionManager commonTm,
                             @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTm) {
        this.devolucionCRUD = devolucionCRUD;
        this.encabezadoFacturaCRUD = encabezadoFacturaCRUD;
        this.detalleFacturaCRUD = detalleFacturaCRUD;
        this.commonJdbc = new JdbcTemplate(commonDS);
        this.commonTx = new TransactionTemplate(commonTm);
        this.tenantTx = new TransactionTemplate(tenantTm);
    }

    // ============================================================
    //                        CREATE
    // ============================================================

    public SaleReturnResponse create(SaleReturnRequest request, Principal principal) {
        String tenant = requireTenant();
        Integer cajaCode = resolveCajaCode(tenant);
        String user = principal != null ? principal.getName() : "SYSTEM";

        // ---- (1) Validar factura existe en la caja del usuario ----
        boolean exists = Boolean.TRUE.equals(tenantTx.execute(s ->
                encabezadoFacturaCRUD.findById(request.invoiceNumber()).isPresent()));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Factura " + request.invoiceNumber() + " no existe en caja " + tenant);
        }

        // ---- (2) Validar cantidad disponible por linea ----
        List<String> errores = new ArrayList<>();
        for (SaleReturnLineRequest line : request.items()) {
            BigDecimal facturada = tenantTx.execute(s ->
                    detalleFacturaCRUD.sumCantidadByFacturaArticulo(
                            request.invoiceNumber(), line.productId()));
            if (facturada == null) facturada = BigDecimal.ZERO;

            BigDecimal yaDevuelta = devolucionCRUD.sumCantidadByFacturaCajaArticulo(
                    request.invoiceNumber(), cajaCode, line.productId());
            if (yaDevuelta == null) yaDevuelta = BigDecimal.ZERO;

            BigDecimal disponible = facturada.subtract(yaDevuelta);
            if (line.quantity().compareTo(disponible) > 0) {
                errores.add(String.format(
                        "articulo %d: solicita devolver %s, pero disponible es %s (facturado=%s, ya devuelto=%s)",
                        line.productId(), line.quantity(), disponible, facturada, yaDevuelta));
            }
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cantidades exceden lo disponible: " + String.join("; ", errores));
        }

        // ---- (3) INSERT batch (commonTM) — trigger dispara crear_dev_venta_kardex ----
        LocalDate today = LocalDate.now();
        List<DetalleDevolucion> savedRows = commonTx.execute(s -> {
            List<DetalleDevolucion> saved = new ArrayList<>();
            for (SaleReturnLineRequest line : request.items()) {
                DetalleDevolucion d = buildDevolucion(line, request.invoiceNumber(), cajaCode, today);
                saved.add(devolucionCRUD.save(d));
            }
            return saved;
        });

        log.info("Sale return: factura={} caja={} user={} items={} totalDevuelto={}",
                request.invoiceNumber(), cajaCode, user, savedRows.size(),
                sumTotal(savedRows));

        // ---- (4) Recargar para obtener agregaKardex=1 ya seteado por el trigger ----
        List<SaleReturnLineResponse> items = savedRows.stream()
                .map(d -> devolucionCRUD.findById(d.getCodigoDevolucion()).orElse(d))
                .map(this::toLineResponse)
                .collect(Collectors.toList());

        return new SaleReturnResponse(
                request.invoiceNumber(),
                cajaCode,
                tenant,
                user,
                today,
                items.stream().map(SaleReturnLineResponse::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                items);
    }

    // ============================================================
    //                        READ
    // ============================================================

    public SaleReturnLineResponse getById(int codigoDevolucion) {
        DetalleDevolucion d = devolucionCRUD.findById(codigoDevolucion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Devolucion " + codigoDevolucion + " no existe"));
        return toLineResponse(d);
    }

    public Page<SaleReturnLineResponse> search(Integer invoiceNumber, LocalDate from, LocalDate to,
                                                int page, int size) {
        return devolucionCRUD.search(invoiceNumber, from, to, PageRequest.of(page, size))
                .map(this::toLineResponse);
    }

    public SaleReturnResponse findByInvoice(int invoiceNumber) {
        String tenant = requireTenant();
        Integer cajaCode = resolveCajaCode(tenant);
        List<DetalleDevolucion> rows = devolucionCRUD.findByNumeroFacturaAndCodigoCaja(invoiceNumber, cajaCode);

        List<SaleReturnLineResponse> items = rows.stream().map(this::toLineResponse).collect(Collectors.toList());
        LocalDate maxFecha = items.stream().map(SaleReturnLineResponse::date)
                .max(LocalDate::compareTo).orElse(null);

        return new SaleReturnResponse(
                invoiceNumber,
                cajaCode,
                tenant,
                null,
                maxFecha,
                items.stream().map(SaleReturnLineResponse::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                items);
    }

    // ============================================================
    //                        helpers
    // ============================================================

    private String requireTenant() {
        String t = TenantContext.getTenant();
        if (t == null || t.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene caja asignada — no puede devolver ventas");
        }
        return t;
    }

    private Integer resolveCajaCode(String tenantDb) {
        try {
            return commonJdbc.queryForObject(
                    "SELECT codigo FROM cajas WHERE nombre_db = ? LIMIT 1",
                    Integer.class, tenantDb);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo resolver codigo_caja para tenant " + tenantDb);
        }
    }

    private DetalleDevolucion buildDevolucion(SaleReturnLineRequest line,
                                              Integer numeroFactura,
                                              Integer codigoCaja,
                                              LocalDate fecha) {
        DetalleDevolucion d = new DetalleDevolucion();
        d.setNumeroFactura(numeroFactura);
        d.setCodigoCaja(codigoCaja);
        d.setCodigoArticulo(line.productId());
        d.setCantidad(line.quantity());
        d.setPrecio(line.price());
        d.setImpuesto(nz(line.tax()));
        d.setDescuento(nz(line.discount()));
        // subtotal y total: si no vienen, calcular desde quantity * price.
        BigDecimal computedSubtotal = line.quantity().multiply(line.price());
        d.setSubtotal(line.subtotal() != null ? line.subtotal() : computedSubtotal);
        d.setTotal(line.total() != null ? line.total() : computedSubtotal.add(nz(line.tax())));
        d.setFecha(fecha);
        d.setAgregaKardex(0);  // el trigger lo cambia a 1
        return d;
    }

    private SaleReturnLineResponse toLineResponse(DetalleDevolucion d) {
        return new SaleReturnLineResponse(
                d.getCodigoDevolucion(),
                d.getNumeroFactura(),
                d.getCodigoCaja(),
                d.getCodigoArticulo(),
                d.getCantidad(),
                d.getPrecio(),
                d.getImpuesto(),
                d.getDescuento(),
                d.getSubtotal(),
                d.getTotal(),
                d.getFecha(),
                d.getAgregaKardex());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal sumTotal(List<DetalleDevolucion> rows) {
        return rows.stream().map(DetalleDevolucion::getTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

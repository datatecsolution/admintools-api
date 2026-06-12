package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.AnnulInvoiceRequest;
import net.datatecsolution.admintools.domain.dto.AnnulInvoiceResponse;
import net.datatecsolution.admintools.domain.dto.ReturnableInvoiceResponse;
import net.datatecsolution.admintools.domain.dto.SaleReturnLineRequest;
import net.datatecsolution.admintools.domain.dto.SaleReturnLineResponse;
import net.datatecsolution.admintools.domain.dto.SaleReturnRequest;
import net.datatecsolution.admintools.domain.dto.SaleReturnResponse;
import net.datatecsolution.admintools.persistence.crud.DetalleDevolucionCRUD;
import net.datatecsolution.admintools.persistence.entity.DetalleDevolucion;
import net.datatecsolution.admintools.persistence.tenant.entity.DetalleFactura;
import net.datatecsolution.admintools.persistence.tenant.entity.EncabezadoFactura;
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
    private final AdminAuthorizationService adminAuth;
    private final AccountsReceivableService accountsReceivable;
    private final net.datatecsolution.admintools.persistence.crud.CajaCRUD cajaCRUD;
    private final JdbcTemplate commonJdbc;
    private final TransactionTemplate commonTx;
    private final TransactionTemplate tenantTx;

    public SaleReturnService(DetalleDevolucionCRUD devolucionCRUD,
                             EncabezadoFacturaCRUD encabezadoFacturaCRUD,
                             DetalleFacturaCRUD detalleFacturaCRUD,
                             AdminAuthorizationService adminAuth,
                             AccountsReceivableService accountsReceivable,
                             net.datatecsolution.admintools.persistence.crud.CajaCRUD cajaCRUD,
                             @Qualifier("commonDataSource") DataSource commonDS,
                             @Qualifier("transactionManager") PlatformTransactionManager commonTm,
                             @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTm) {
        this.devolucionCRUD = devolucionCRUD;
        this.encabezadoFacturaCRUD = encabezadoFacturaCRUD;
        this.detalleFacturaCRUD = detalleFacturaCRUD;
        this.adminAuth = adminAuth;
        this.accountsReceivable = accountsReceivable;
        this.cajaCRUD = cajaCRUD;
        this.commonJdbc = new JdbcTemplate(commonDS);
        this.commonTx = new TransactionTemplate(commonTm);
        this.tenantTx = new TransactionTemplate(tenantTm);
    }

    /**
     * Resuelve la caja a usar: si viene {@code cajaParam} (admin/supervisor que
     * eligió una caja en la sección Facturas), fija el TenantContext a esa caja
     * para que el EMF de tenant consulte su BD y devuelve su código. Si no, usa
     * el tenant del usuario (cajero con caja asignada).
     */
    private Integer resolveCaja(Integer cajaParam) {
        if (cajaParam != null) {
            String db = cajaCRUD.findById(cajaParam)
                    .map(c -> c.getNombreDb())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caja no encontrada"));
            TenantContext.setTenant(db);
            return cajaParam;
        }
        return resolveCajaCode(requireTenant());
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
    //              US-041: anulación (parcial/total)
    // ============================================================

    /** Factura con sus líneas devolvibles (facturada − ya devuelta) para el modal. */
    public ReturnableInvoiceResponse getReturnable(int invoiceNumber, Integer cajaParam) {
        Integer cajaCode = resolveCaja(cajaParam);
        EncabezadoFactura header = tenantTx.execute(s ->
                encabezadoFacturaCRUD.findById(invoiceNumber).orElse(null));
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Factura " + invoiceNumber + " no existe en la caja " + cajaCode);
        }
        // Agregar líneas por artículo (precio y impuesto unitarios + cantidad).
        List<DetalleFactura> lineas = tenantTx.execute(s -> detalleFacturaCRUD.findByNumeroFactura(invoiceNumber));
        java.util.Map<Integer, BigDecimal[]> porArticulo = new java.util.LinkedHashMap<>();
        for (DetalleFactura d : lineas) {
            // [precioUnit, impuestoTotal, cantidad]
            BigDecimal[] acc = porArticulo.computeIfAbsent(d.getCodigoArticulo(),
                    k -> new BigDecimal[]{d.getPrecio(), BigDecimal.ZERO, BigDecimal.ZERO});
            acc[1] = acc[1].add(nz(d.getImpuesto()));
            acc[2] = acc[2].add(nz(d.getCantidad()));
        }
        java.util.Map<Integer, String> nombres = nombresArticulos(porArticulo.keySet());
        List<ReturnableInvoiceResponse.ReturnableLine> out = new ArrayList<>();
        for (var e : porArticulo.entrySet()) {
            int art = e.getKey();
            BigDecimal precio = e.getValue()[0];
            BigDecimal facturada = e.getValue()[2];
            BigDecimal impUnit = facturada.signum() > 0
                    ? e.getValue()[1].divide(facturada, 6, java.math.RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            BigDecimal devuelta = nz(devolucionCRUD.sumCantidadByFacturaCajaArticulo(invoiceNumber, cajaCode, art));
            out.add(new ReturnableInvoiceResponse.ReturnableLine(
                    art, nombres.getOrDefault(art, "Art. " + art), precio,
                    impUnit.setScale(2, java.math.RoundingMode.HALF_EVEN),
                    facturada, devuelta, facturada.subtract(devuelta)));
        }
        return new ReturnableInvoiceResponse(invoiceNumber, cajaCode,
                header.getEstadoFactura(), header.getTipoFactura(), nz(header.getTotal()), out);
    }

    /** Anula la factura: devuelve mercadería (kardex) y, si es total + crédito, reversa CxC. */
    public AnnulInvoiceResponse annul(int invoiceNumber, Integer cajaParam, AnnulInvoiceRequest req, Principal principal) {
        if (!adminAuth.verifyAdminPassword(req.supervisorPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave de supervisor inválida");
        }
        Integer cajaCode = resolveCaja(cajaParam);
        String user = principal != null ? principal.getName() : "SYSTEM";

        EncabezadoFactura header = tenantTx.execute(s ->
                encabezadoFacturaCRUD.findById(invoiceNumber).orElse(null));
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Factura " + invoiceNumber + " no existe en la caja " + cajaCode);
        }
        if ("NULA".equalsIgnoreCase(header.getEstadoFactura())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La factura ya está anulada");
        }

        // Precio/impuesto unitario por artículo (para construir las devoluciones).
        List<DetalleFactura> detalle = tenantTx.execute(s -> detalleFacturaCRUD.findByNumeroFactura(invoiceNumber));
        java.util.Map<Integer, BigDecimal[]> info = new java.util.LinkedHashMap<>();
        for (DetalleFactura d : detalle) {
            BigDecimal[] acc = info.computeIfAbsent(d.getCodigoArticulo(),
                    k -> new BigDecimal[]{d.getPrecio(), BigDecimal.ZERO, BigDecimal.ZERO});
            acc[1] = acc[1].add(nz(d.getImpuesto()));
            acc[2] = acc[2].add(nz(d.getCantidad()));
        }

        // Determinar qué líneas/cantidades devolver.
        List<SaleReturnLineRequest> aDevolver = new ArrayList<>();
        if (req.total()) {
            // Todo lo que falte por devolver de cada artículo.
            for (var e : info.entrySet()) {
                int art = e.getKey();
                BigDecimal facturada = e.getValue()[2];
                BigDecimal devuelta = nz(devolucionCRUD.sumCantidadByFacturaCajaArticulo(invoiceNumber, cajaCode, art));
                BigDecimal resto = facturada.subtract(devuelta);
                if (resto.signum() > 0) aDevolver.add(lineaDevolucion(art, resto, e.getValue()));
            }
        } else {
            if (req.lineas() == null || req.lineas().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Anulación parcial sin líneas");
            }
            for (AnnulInvoiceRequest.AnnulLine l : req.lineas()) {
                BigDecimal[] meta = info.get(l.codigoArticulo());
                if (meta == null) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "El artículo " + l.codigoArticulo() + " no está en la factura");
                }
                BigDecimal devuelta = nz(devolucionCRUD.sumCantidadByFacturaCajaArticulo(invoiceNumber, cajaCode, l.codigoArticulo()));
                BigDecimal disponible = meta[2].subtract(devuelta);
                if (l.cantidad().signum() <= 0 || l.cantidad().compareTo(disponible) > 0) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Artículo " + l.codigoArticulo() + ": cantidad inválida (disponible " + disponible + ")");
                }
                aDevolver.add(lineaDevolucion(l.codigoArticulo(), l.cantidad(), meta));
            }
        }

        // (1) Registrar devoluciones (commonTx) → trigger reversa kardex.
        LocalDate today = LocalDate.now();
        BigDecimal totalDevuelto = BigDecimal.ZERO;
        if (!aDevolver.isEmpty()) {
            List<DetalleDevolucion> saved = commonTx.execute(s -> {
                List<DetalleDevolucion> rows = new ArrayList<>();
                for (SaleReturnLineRequest l : aDevolver) {
                    rows.add(devolucionCRUD.save(buildDevolucion(l, invoiceNumber, cajaCode, today)));
                }
                return rows;
            });
            totalDevuelto = sumTotal(saved);
        }

        // (2) Solo en TOTAL: estado NULA + motivo, y reverso CxC si es crédito.
        boolean cxcReversada = false;
        BigDecimal abonoCxc = BigDecimal.ZERO;
        String estadoFinal = header.getEstadoFactura();
        if (req.total()) {
            tenantTx.executeWithoutResult(s -> {
                EncabezadoFactura h = encabezadoFacturaCRUD.findById(invoiceNumber).orElseThrow();
                h.setEstadoFactura("NULA");
                h.setObservacion(("ANULADA: " + req.motivo().trim() + " — por " + user));
                encabezadoFacturaCRUD.save(h);
            });
            estadoFinal = "NULA";
            if (header.getTipoFactura() != null && header.getTipoFactura() == 2) {
                int customerId = parseClienteId(header.getCodigoCliente());
                if (customerId > 0) {
                    abonoCxc = accountsReceivable.reverseCreditInvoice(customerId, cajaCode, invoiceNumber, user);
                    cxcReversada = abonoCxc.signum() > 0;
                }
            }
        }

        log.info("Anulación factura {} caja {} user {} total={} devuelto={} cxc={}",
                invoiceNumber, cajaCode, user, req.total(), totalDevuelto, abonoCxc);
        return new AnnulInvoiceResponse(invoiceNumber, req.total(), estadoFinal,
                totalDevuelto, cxcReversada, abonoCxc);
    }

    private SaleReturnLineRequest lineaDevolucion(int articulo, BigDecimal cantidad, BigDecimal[] meta) {
        BigDecimal precio = meta[0];
        BigDecimal facturada = meta[2];
        BigDecimal impUnit = facturada.signum() > 0
                ? meta[1].divide(facturada, 6, java.math.RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
        BigDecimal impuesto = impUnit.multiply(cantidad).setScale(2, java.math.RoundingMode.HALF_EVEN);
        return new SaleReturnLineRequest(articulo, cantidad, precio, impuesto, BigDecimal.ZERO, null, null);
    }

    private java.util.Map<Integer, String> nombresArticulos(java.util.Set<Integer> codigos) {
        if (codigos.isEmpty()) return java.util.Map.of();
        String in = codigos.stream().map(String::valueOf).collect(Collectors.joining(","));
        java.util.Map<Integer, String> m = new java.util.HashMap<>();
        commonJdbc.query("SELECT codigo_articulo, articulo FROM articulo WHERE codigo_articulo IN (" + in + ")",
                rs -> { m.put(rs.getInt(1), rs.getString(2)); });
        return m;
    }

    private static int parseClienteId(String codigo) {
        try { return codigo == null ? 0 : Integer.parseInt(codigo.trim()); }
        catch (NumberFormatException e) { return 0; }
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

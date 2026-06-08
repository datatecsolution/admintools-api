package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.core.FacturacionCalculadora;
import net.datatecsolution.admintools.domain.dto.InvoiceCreateRequest;
import net.datatecsolution.admintools.domain.dto.InvoiceLineRequest;
import net.datatecsolution.admintools.domain.dto.InvoiceLineResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceResponse;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigAppCRUD;
import net.datatecsolution.admintools.persistence.crud.ImpuestoCRUD;
import net.datatecsolution.admintools.persistence.crud.OrdenCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.DetalleOrden;
import net.datatecsolution.admintools.persistence.entity.Orden;
import net.datatecsolution.admintools.persistence.tenant.crud.DatosFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.crud.DetalleFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.crud.EncabezadoFacturaCRUD;
import net.datatecsolution.admintools.persistence.tenant.entity.DetalleFactura;
import net.datatecsolution.admintools.persistence.tenant.entity.EncabezadoFactura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * INV-8 / US-020 — Facturacion definitiva desde orden temporal.
 *
 * Flujo {@link #createFromOrder}:
 *
 *   0) TenantContext debe estar poblado (TenantInterceptor de US-017). Sin
 *      caja asignada -> 403.
 *
 *   1) UPDATE optimista en {@code admin_tools.encabezado_factura_temp}:
 *      estado 1 -> 3. Si la fila ya estaba en 3 (alguien la facturo entre
 *      la lectura y el update), abortamos 409. Garantiza que la misma orden
 *      no se facture dos veces concurrentemente (cierra la ventana del
 *      read-modify-write entre validar y marcar). El UPDATE corre en
 *      transaccion del commonTM.
 *
 *   2) INSERT en {@code admin_tools_caja_N.encabezado_factura} + N
 *      {@code detalle_factura}. Cada INSERT de detalle dispara el trigger
 *      {@code detalle_factura_b_insert} (V8 caja Swing) -> llama
 *      {@code admin_tools.crear_venta_kardex} -> el kardex y
 *      {@code existencia_articulo_bodega} bajan automaticamente. Esto corre
 *      en transaccion del tenantTM (BD distinta a commonTM).
 *
 *   COMPENSACION: si el paso 2 falla, revertimos la orden a estado=1. Asi
 *   el usuario puede reintentar la facturacion. Si la compensacion tambien
 *   falla, se loguea ERROR — caso para alerta humana. La ventana es
 *   pequenia (entre el commit del UPDATE y el insert de la factura).
 *
 *   No usamos XA porque MySQL multi-DataSource via Spring no lo soporta
 *   limpiamente y el escenario es controlable: la unica operacion
 *   destructiva del paso 2 es el kardex (via trigger), pero si el insert
 *   de detalle_factura falla, la transaccion tenant hace rollback y el
 *   trigger NO se commitea (los CALL al SP corren dentro de la tx del
 *   INSERT). Asi que paso 2 es atomico.
 *
 * Esta clase NO toca {@link OrdenCRUD#save} para el UPDATE — usa el bean
 * save() pero envuelto en TransactionTemplate sobre commonTM para no
 * heredar la transaccion del tenantTM si algun caller la propagara.
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final OrdenCRUD ordenCRUD;
    private final ClienteCRUD clienteCRUD;
    private final EncabezadoFacturaCRUD encabezadoFacturaCRUD;
    private final DetalleFacturaCRUD detalleFacturaCRUD;
    private final CajaCRUD cajaCRUD;
    private final ConfigAppCRUD configAppCRUD;
    private final ImpuestoCRUD impuestoCRUD;
    private final DatosFacturaCRUD datosFacturaCRUD;
    private final AccountsReceivableService accountsReceivableService;
    private final SellerCatalogService sellerCatalogService;
    private final TransactionTemplate commonTx;
    private final TransactionTemplate tenantTx;

    public InvoiceService(OrdenCRUD ordenCRUD,
                          ClienteCRUD clienteCRUD,
                          EncabezadoFacturaCRUD encabezadoFacturaCRUD,
                          DetalleFacturaCRUD detalleFacturaCRUD,
                          CajaCRUD cajaCRUD,
                          ConfigAppCRUD configAppCRUD,
                          ImpuestoCRUD impuestoCRUD,
                          DatosFacturaCRUD datosFacturaCRUD,
                          AccountsReceivableService accountsReceivableService,
                          SellerCatalogService sellerCatalogService,
                          @Qualifier("transactionManager") PlatformTransactionManager commonTm,
                          @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTm) {
        this.ordenCRUD = ordenCRUD;
        this.clienteCRUD = clienteCRUD;
        this.encabezadoFacturaCRUD = encabezadoFacturaCRUD;
        this.detalleFacturaCRUD = detalleFacturaCRUD;
        this.cajaCRUD = cajaCRUD;
        this.configAppCRUD = configAppCRUD;
        this.impuestoCRUD = impuestoCRUD;
        this.datosFacturaCRUD = datosFacturaCRUD;
        this.accountsReceivableService = accountsReceivableService;
        this.sellerCatalogService = sellerCatalogService;
        this.commonTx = new TransactionTemplate(commonTm);
        this.tenantTx = new TransactionTemplate(tenantTm);
    }

    public InvoiceResponse createFromOrder(int orderId, Principal principal) {
        String tenant = requireTenant();
        String user = principal != null ? principal.getName() : "SYSTEM";

        // ---- (1) UPDATE optimista en orden temp + carga full ----
        Orden orden = commonTx.execute(status -> {
            Orden o = ordenCRUD.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Orden " + orderId + " no existe"));
            if (o.getEstado() == null || o.getEstado() != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Orden " + orderId + " no esta pendiente (estado=" + o.getEstado() + ")");
            }
            if (o.getDetalles() == null || o.getDetalles().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Orden " + orderId + " no tiene detalles para facturar");
            }
            o.getDetalles().size();  // fuerza carga lazy dentro de la tx
            o.setEstado(3);
            return ordenCRUD.save(o);
        });

        // ---- (2) INSERT factura + lineas en la caja (trigger dispara kardex) ----
        EncabezadoFactura savedHeader;
        try {
            savedHeader = tenantTx.execute(status -> {
                EncabezadoFactura header = buildHeader(orden, user);
                EncabezadoFactura sh = encabezadoFacturaCRUD.save(header);

                for (DetalleOrden lo : orden.getDetalles()) {
                    DetalleFactura line = buildLine(lo, sh.getNumeroFactura());
                    detalleFacturaCRUD.save(line);  // <- trigger detalle_factura_b_insert dispara
                }
                return sh;
            });
        } catch (RuntimeException e) {
            log.error("INV-8: insercion factura desde orden={} fallo en tenant={}, compensando estado",
                    orderId, tenant, e);
            try {
                commonTx.executeWithoutResult(s ->
                        ordenCRUD.findById(orderId).ifPresent(o -> {
                            o.setEstado(1);
                            ordenCRUD.save(o);
                        }));
                log.info("INV-8: orden {} revertida a estado=1 OK", orderId);
            } catch (Exception ex2) {
                log.error("INV-8: COMPENSACION FALLO — orden {} quedo en estado 3 sin factura. Reconciliar manualmente.",
                        orderId, ex2);
            }
            throw e;
        }

        // ---- (3) US-034: venta a credito -> generar saldo en CxC (BD comun) ----
        if (orden.getTipoFactura() != null && orden.getTipoFactura() == 2) {
            postCreditCxc(orden, savedHeader.getNumeroFactura(), tenant, user);
        }

        return loadResponse(savedHeader.getNumeroFactura(), tenant);
    }

    /**
     * Checkout POS — crea la factura DIRECTO desde el carrito (sin orden),
     * fiel al Swing {@code FacturaDao.registrar} + {@code FacturacionService}:
     * totales con precio-incluye-ISV (FacturacionCalculadora), encabezado +
     * detalle en la caja (trigger del kardex) y, si es credito, saldo CxC.
     */
    public InvoiceResponse createDirect(InvoiceCreateRequest req, Principal principal) {
        String tenant = requireTenant();
        String user = principal != null ? principal.getName() : "SYSTEM";
        boolean credito = req.tipoFactura() != null && req.tipoFactura() == 2;

        // Resolver cliente (fiel al Swing): id valido -> ese; si no y hay nombre
        // escrito -> registrar contado (tipo 1) al vuelo; si no -> Consumidor final.
        Cliente cliente = resolveCustomer(req);
        int customerId = cliente.getId();

        // Credito: solo clientes gestionados (tipo 2). Contado/escritos (tipo 1) no.
        if (credito && (cliente.getTipoCliente() == null || cliente.getTipoCliente() != 2)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El credito solo aplica a clientes registrados (gestionados); "
                    + "el cliente seleccionado es de contado.");
        }

        // % ISV por taxId (autoritativo, server-side)
        Map<Integer, Integer> percentByTax = percentByTax();

        // Totales fieles al Swing (precio incluye ISV) via modulo compartido
        List<FacturacionCalculadora.LineaInput> calcLines = req.lines().stream()
                .map(l -> new FacturacionCalculadora.LineaInput(
                        l.cantidad(), l.precioUnitario(), nz(l.descuento()),
                        percentByTax.getOrDefault(l.taxId(), 0), 0))
                .collect(Collectors.toList());
        FacturacionCalculadora.Totales tot = FacturacionCalculadora.calcularTotales(calcLines);

        int codigoCaja = cajaCRUD.findByNombreDb(tenant).map(c -> c.getCodigo()).orElse(0);
        int dueDays = creditDueDays();
        // Vendedor: segun config_user_facturacion.ventana_vendedor (Swing). Default 1.
        int codigoVendedor = sellerCatalogService.resolveVendedor(user, req.vendedorId());

        EncabezadoFactura savedHeader = tenantTx.execute(status -> {
            int codRango = datosFacturaCRUD.findTopByOrderByCodigoRangoDesc()
                    .map(d -> d.getCodigoRango()).orElse(1);
            EncabezadoFactura header = buildHeaderDirect(req, customerId, tot, user, credito, codRango, dueDays, codigoVendedor);
            EncabezadoFactura sh = encabezadoFacturaCRUD.save(header);
            for (InvoiceLineRequest l : req.lines()) {
                detalleFacturaCRUD.save(buildLineDirect(l, sh.getNumeroFactura(),
                        percentByTax.getOrDefault(l.taxId(), 0)));  // trigger kardex
            }
            return sh;
        });

        if (credito) {
            try {
                accountsReceivableService.postCreditSale(customerId, codigoCaja,
                        savedHeader.getNumeroFactura(), tot.getTotal(),
                        LocalDate.now().plusDays(dueDays), user);
            } catch (RuntimeException e) {
                log.error("POS: factura {} (caja={}, cliente={}) quedo SIN saldo CxC — reconciliar manualmente",
                        savedHeader.getNumeroFactura(), codigoCaja, customerId, e);
                throw e;
            }
        }

        return loadResponse(savedHeader.getNumeroFactura(), tenant);
    }

    private static final int CLIENTE_CONSUMIDOR_FINAL = 1;
    private static final int TIPO_CLIENTE_CONTADO = 1;

    /**
     * Resuelve el cliente de la factura (fiel al Swing {@code registrarClienteContado}):
     * <ul>
     *   <li>{@code customerId} válido (&gt;0) → ese cliente;</li>
     *   <li>si no y hay {@code customerName} → registra un cliente de contado
     *       (tipo 1) al vuelo y factura a él (no aparece en el admin);</li>
     *   <li>si no → Consumidor final (id 1).</li>
     * </ul>
     */
    private Cliente resolveCustomer(InvoiceCreateRequest req) {
        Integer id = req.customerId();
        if (id != null && id > 0) {
            return clienteCRUD.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        }
        String nombre = req.customerName() == null ? "" : req.customerName().trim();
        if (nombre.isEmpty()) {
            return clienteCRUD.findById(CLIENTE_CONSUMIDOR_FINAL)
                    .orElseThrow(() -> new EntityNotFoundException("Consumidor final (id 1) no existe"));
        }
        Cliente nuevo = new Cliente();
        nuevo.setNombre(nombre);
        nuevo.setTipoCliente(TIPO_CLIENTE_CONTADO);
        nuevo.setIdVendedor(1);
        return commonTx.execute(s -> clienteCRUD.save(nuevo));
    }

    /** Mapa taxId -> porcentaje (int) desde el catalogo de impuestos. */
    private Map<Integer, Integer> percentByTax() {
        Map<Integer, Integer> map = new java.util.HashMap<>();
        impuestoCRUD.findAll().forEach(i -> map.put(i.getId(), parsePercent(i.getPorcentaje())));
        return map;
    }

    private static int parsePercent(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return new BigDecimal(raw.trim()).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private EncabezadoFactura buildHeaderDirect(InvoiceCreateRequest req,
                                                int customerId,
                                                FacturacionCalculadora.Totales tot,
                                                String user, boolean credito,
                                                int codRango, int dueDays, int codigoVendedor) {
        EncabezadoFactura h = new EncabezadoFactura();
        h.setFecha(LocalDateTime.now());
        h.setSubtotalExcento(tot.getSubtotalExcento());
        h.setSubtotal15(tot.getSubtotal15());
        h.setSubtotal18(tot.getSubtotal18());
        h.setSubtotal(tot.getSubtotal());
        h.setImpuesto(tot.getImpuesto());
        h.setIsv18(tot.getIsv18());
        h.setIsvOtros(tot.getOtrosImpuestos());
        h.setTotal(tot.getTotal());
        h.setDescuento(tot.getDescuento());
        h.setCodigoCliente(String.valueOf(customerId));
        h.setCodigo("1");                   // mirror Swing (FacturaDao)
        h.setEstadoFactura("ACT");
        h.setUsuario(user);
        h.setAgregaKardex(0);
        h.setCodigoVendedor(codigoVendedor); // empleado/vendedor (config ventana_vendedor; default 1)
        h.setCodRango(codRango);            // CAI activo de la caja
        h.setTotalLetras("NA");
        h.setObservacion(req.observacion() == null || req.observacion().isBlank()
                ? "NA" : req.observacion());
        h.setFechaVencimiento(LocalDate.now().plusDays(dueDays));
        h.setTipoFactura(credito ? 2 : 1);
        if (credito) {
            h.setTipoPago(3);
            h.setPago(BigDecimal.ZERO);
            h.setEstadoPago(0);             // pendiente (Swing)
            h.setCobroEfectivo(BigDecimal.ZERO);
            h.setCobroTarjeta(BigDecimal.ZERO);
        } else {
            h.setTipoPago(req.tipoPago() != null ? req.tipoPago() : 1);
            h.setPago(tot.getTotal());
            h.setEstadoPago(1);             // pagada (Swing)
            h.setCobroEfectivo(nz(req.cobroEfectivo()));
            h.setCobroTarjeta(nz(req.cobroTarjeta()));
        }
        return h;
    }

    private DetalleFactura buildLineDirect(InvoiceLineRequest l, Integer numeroFactura, int percent) {
        BigDecimal desc = nz(l.descuento()).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        BigDecimal totalItem = l.cantidad().multiply(l.precioUnitario()).subtract(desc);
        BigDecimal porImp = new BigDecimal(percent).divide(new BigDecimal(100)).add(BigDecimal.ONE);
        BigDecimal base = totalItem.divide(porImp, 2, BigDecimal.ROUND_HALF_EVEN);
        BigDecimal impuesto = totalItem.subtract(base);

        DetalleFactura d = new DetalleFactura();
        d.setNumeroFactura(numeroFactura);
        d.setCodigoArticulo(l.productId());
        d.setPrecio(l.precioUnitario());
        d.setCantidad(l.cantidad());
        d.setImpuesto(impuesto.setScale(2, BigDecimal.ROUND_HALF_EVEN));
        d.setSubtotal(base);
        d.setDescuento(desc);
        d.setTotal(totalItem.setScale(2, BigDecimal.ROUND_HALF_EVEN));
        d.setCodigoBarra("NA");
        d.setAgregaKardex(0);  // trigger lo cambia a 1
        return d;
    }

    /**
     * US-034: tras commitear la factura a credito en la caja, registra el saldo
     * en las tablas CxC de la BD comun. Si falla, la factura ya esta commiteada
     * (no hay XA): logueamos para reconciliacion manual y propagamos el error.
     */
    private void postCreditCxc(Orden orden, Integer numeroFactura, String tenant, String user) {
        Integer customerId = orden.getClienteId();
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Una venta a credito requiere un cliente valido");
        }
        int codigoCaja = cajaCRUD.findByNombreDb(tenant).map(c -> c.getCodigo()).orElse(0);
        LocalDate fechaVenc = LocalDate.now().plusDays(creditDueDays());
        try {
            accountsReceivableService.postCreditSale(
                    customerId, codigoCaja, numeroFactura, nz(orden.getTotal()), fechaVenc, user);
        } catch (RuntimeException e) {
            log.error("US-034: factura {} (caja={}, cliente={}) quedo SIN saldo CxC — reconciliar manualmente",
                    numeroFactura, codigoCaja, customerId, e);
            throw e;
        }
    }

    /** Dias de credito desde config_app; 0 si no hay config o falla la lectura. */
    private int creditDueDays() {
        try {
            return configAppCRUD.findDiaVencimientoFactura().orElse(0);
        } catch (RuntimeException e) {
            log.warn("US-034: no se pudo leer config_app.dia_vencimiento_factura, usando 0: {}", e.getMessage());
            return 0;
        }
    }

    public InvoiceResponse getById(int invoiceId) {
        String tenant = requireTenant();
        return loadResponse(invoiceId, tenant);
    }

    public Page<InvoiceResponse> search(int page, int size) {
        String tenant = requireTenant();
        Page<EncabezadoFactura> headers = encabezadoFacturaCRUD
                .findAllByOrderByFechaDesc(PageRequest.of(page, size));

        // Cache de nombres de cliente para no martillar admin_tools.cliente:
        // muchas facturas suelen tener pocos clientes distintos.
        Map<Integer, String> nameCache = new ConcurrentHashMap<>();
        return headers.map(h -> toResponse(h, loadLines(h.getNumeroFactura()), tenant, nameCache));
    }

    // ============================================================
    //                        helpers
    // ============================================================

    private String requireTenant() {
        String t = TenantContext.getTenant();
        if (t == null || t.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene caja asignada — no puede facturar");
        }
        return t;
    }

    private EncabezadoFactura buildHeader(Orden orden, String user) {
        EncabezadoFactura h = new EncabezadoFactura();
        h.setFecha(LocalDateTime.now());
        h.setSubtotalExcento(nz(orden.getSubTotalExcento()));
        h.setSubtotal15(nz(orden.getSubTotal15()));
        h.setSubtotal18(nz(orden.getSubTotal18()));
        h.setSubtotal(nz(orden.getSubTotal()));
        h.setImpuesto(nz(orden.getTotalImpuesto()));
        h.setTotal(nz(orden.getTotal()));
        // caja: codigo_cliente VARCHAR(18) — convertir
        h.setCodigoCliente(orden.getClienteId() == null ? "0" : orden.getClienteId().toString());
        h.setCodigo("NA");                  // CAI: manejaremos en historia aparte
        h.setEstadoFactura("ACT");
        h.setIsvOtros(nz(orden.getIsvOtros()));
        h.setIsv18(nz(orden.getTotalImpuesto18()));
        h.setUsuario(user);
        h.setDescuento(nz(orden.getTotalDescuento()));
        // US-034: tipo_factura 2 = credito (lo trae la orden); 1 = contado.
        boolean credito = orden.getTipoFactura() != null && orden.getTipoFactura() == 2;
        h.setTipoFactura(credito ? 2 : 1);
        h.setAgregaKardex(0);
        h.setTipoPago(1);
        h.setObservacion(orden.getObservacion() == null || orden.getObservacion().isBlank()
                ? "NA" : orden.getObservacion());
        h.setTotalLetras("NA");             // NumberToLetter: mejora futura
        h.setCodigoVendedor(orden.getVendedorCod());
        h.setCodRango(1);
        h.setCobroTarjeta(BigDecimal.ZERO);
        if (credito) {
            // venta a credito: nada cobrado al facturar; queda pendiente y con
            // vencimiento = hoy + config_app.dia_vencimiento_factura.
            // estado_pago semantica Swing: 0 = pendiente.
            h.setPago(BigDecimal.ZERO);
            h.setEstadoPago(0);             // pendiente de pago (Swing)
            h.setCobroEfectivo(BigDecimal.ZERO);
            h.setFechaVencimiento(LocalDate.now().plusDays(creditDueDays()));
        } else {
            // contado: pago = total. estado_pago semantica Swing: 1 = pagada.
            h.setPago(nz(orden.getTotal()));
            h.setEstadoPago(1);             // pagada (Swing)
            h.setCobroEfectivo(nz(orden.getTotal()));
            h.setFechaVencimiento(LocalDate.now());
        }
        return h;
    }

    private DetalleFactura buildLine(DetalleOrden lo, Integer numeroFactura) {
        DetalleFactura d = new DetalleFactura();
        d.setNumeroFactura(numeroFactura);
        d.setCodigoArticulo(lo.getCodigoArt());
        d.setPrecio(lo.getPrecioVentaItem() == null
                ? BigDecimal.ZERO : BigDecimal.valueOf(lo.getPrecioVentaItem()));
        d.setCantidad(nz(lo.getCantidad()));
        d.setImpuesto(nz(lo.getImpuesto()));
        d.setSubtotal(nz(lo.getSubTotal()));
        d.setDescuento(nz(lo.getDescuentoItem()));
        d.setTotal(nz(lo.getTotal()));
        d.setCodigoBarra("NA");
        d.setAgregaKardex(0);  // trigger lo cambia a 1
        return d;
    }

    private InvoiceResponse loadResponse(Integer invoiceId, String tenant) {
        EncabezadoFactura header = encabezadoFacturaCRUD.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Factura " + invoiceId + " no existe en tenant " + tenant));
        return toResponse(header, loadLines(invoiceId), tenant, new ConcurrentHashMap<>());
    }

    private List<DetalleFactura> loadLines(Integer numeroFactura) {
        return detalleFacturaCRUD.findByNumeroFactura(numeroFactura);
    }

    private InvoiceResponse toResponse(EncabezadoFactura h,
                                       List<DetalleFactura> lines,
                                       String tenant,
                                       Map<Integer, String> nameCache) {
        Integer customerId = parseIntOrNull(h.getCodigoCliente());
        String customerName = customerId == null ? null
                : nameCache.computeIfAbsent(customerId, this::lookupCustomerName);

        List<InvoiceLineResponse> lineResponses = lines.stream()
                .map(l -> new InvoiceLineResponse(
                        l.getId(),
                        l.getCodigoArticulo(),
                        l.getCantidad(),
                        l.getPrecio(),
                        l.getImpuesto(),
                        l.getDescuento(),
                        l.getSubtotal(),
                        l.getTotal()))
                .collect(Collectors.toList());

        return new InvoiceResponse(
                h.getNumeroFactura(),
                tenant,
                h.getFecha(),
                customerId,
                customerName,
                h.getCodigoVendedor(),
                h.getUsuario(),
                h.getEstadoFactura(),
                h.getTipoFactura(),
                h.getTipoPago(),
                h.getSubtotal(),
                h.getImpuesto(),
                h.getDescuento(),
                h.getTotal(),
                h.getPago(),
                h.getObservacion(),
                lineResponses);
    }

    private String lookupCustomerName(Integer customerId) {
        Optional<Cliente> c = clienteCRUD.findById(customerId);
        return c.map(Cliente::getNombre).orElse(null);
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

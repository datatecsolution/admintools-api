package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.config.TenantContext;
import net.datatecsolution.admintools.domain.dto.InvoiceLineResponse;
import net.datatecsolution.admintools.domain.dto.InvoiceResponse;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.crud.OrdenCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.entity.DetalleOrden;
import net.datatecsolution.admintools.persistence.entity.Orden;
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
    private final TransactionTemplate commonTx;
    private final TransactionTemplate tenantTx;

    public InvoiceService(OrdenCRUD ordenCRUD,
                          ClienteCRUD clienteCRUD,
                          EncabezadoFacturaCRUD encabezadoFacturaCRUD,
                          DetalleFacturaCRUD detalleFacturaCRUD,
                          @Qualifier("transactionManager") PlatformTransactionManager commonTm,
                          @Qualifier("tenantTransactionManager") PlatformTransactionManager tenantTm) {
        this.ordenCRUD = ordenCRUD;
        this.clienteCRUD = clienteCRUD;
        this.encabezadoFacturaCRUD = encabezadoFacturaCRUD;
        this.detalleFacturaCRUD = detalleFacturaCRUD;
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

        return loadResponse(savedHeader.getNumeroFactura(), tenant);
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
        // Asumimos pago al contado: pago = total. tipo_factura/tipo_pago=1.
        h.setPago(nz(orden.getTotal()));
        h.setDescuento(nz(orden.getTotalDescuento()));
        h.setTipoFactura(1);
        h.setAgregaKardex(0);
        h.setTipoPago(1);
        h.setObservacion(orden.getObservacion() == null || orden.getObservacion().isBlank()
                ? "NA" : orden.getObservacion());
        h.setTotalLetras("NA");             // NumberToLetter: mejora futura
        h.setCodigoVendedor(orden.getVendedorCod());
        h.setEstadoPago(0);                 // pagada
        h.setCodRango(1);
        h.setCobroTarjeta(BigDecimal.ZERO);
        h.setCobroEfectivo(nz(orden.getTotal()));
        h.setFechaVencimiento(LocalDate.now());
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

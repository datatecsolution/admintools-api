package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.AnnulPurchaseRequest;
import net.datatecsolution.admintools.domain.dto.AnnulPurchaseResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseLineRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseLineResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleDevolucionCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleFacturaCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.EncabezadoFacturaCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.ProveedorCRUD;
import net.datatecsolution.admintools.persistence.entity.DetalleDevolucionCompra;
import net.datatecsolution.admintools.persistence.entity.DetalleFacturaCompra;
import net.datatecsolution.admintools.persistence.entity.EncabezadoFacturaCompra;
import net.datatecsolution.admintools.persistence.mapper.PurchaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Crea y consulta compras (INV-5). El kardex y el balance materializado
 * los maneja la BD via trigger en {@code detalle_factura_compra} — este
 * service solo inserta los documentos en una transaccion.
 */
@Service
public class PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

    @Autowired private EncabezadoFacturaCompraCRUD headerCrud;
    @Autowired private DetalleFacturaCompraCRUD lineCrud;
    @Autowired private DetalleDevolucionCompraCRUD returnCrud;
    @Autowired private ProveedorCRUD supplierCrud;
    @Autowired private BodegaCRUD warehouseCrud;
    @Autowired private ArticuloMasterCRUD productCrud;
    @Autowired private AdminAuthorizationService adminAuth;
    @Autowired private PurchaseMapper mapper;

    @Transactional
    public PurchaseResponse create(PurchaseRequest request, Principal principal) {
        // 1) Validar FKs
        if (!supplierCrud.existsById(request.supplierId())) {
            throw new EntityNotFoundException("Supplier " + request.supplierId() + " not found");
        }
        if (!warehouseCrud.existsById(request.warehouseCode())) {
            throw new EntityNotFoundException("Warehouse " + request.warehouseCode() + " not found");
        }
        for (PurchaseLineRequest line : request.lines()) {
            if (!productCrud.existsById(line.productId())) {
                throw new EntityNotFoundException("Product " + line.productId() + " not found");
            }
        }

        // 2) Calcular totales server-side
        BigDecimal subtotalSum = BigDecimal.ZERO;
        BigDecimal taxSum      = BigDecimal.ZERO;
        for (PurchaseLineRequest line : request.lines()) {
            BigDecimal lineSubtotal = line.price().multiply(line.quantity());
            subtotalSum = subtotalSum.add(lineSubtotal);
            taxSum      = taxSum.add(line.tax() != null ? line.tax() : BigDecimal.ZERO);
        }
        BigDecimal totalSum = subtotalSum.add(taxSum);

        // 3) Construir encabezado con defaults sensatos
        EncabezadoFacturaCompra header = new EncabezadoFacturaCompra();
        LocalDate fecha = request.date() != null ? request.date() : LocalDate.now();
        header.setFecha(fecha);
        header.setSubtotalExcento(BigDecimal.ZERO);
        header.setSubtotal15(subtotalSum);              // simplificacion: todo en 15%
        header.setSubtotal18(BigDecimal.ZERO);
        header.setSubtotal(subtotalSum);
        header.setImpuesto(taxSum);
        header.setTotal(totalSum);
        header.setCodigoProveedor(request.supplierId());
        header.setCodigo(request.invoiceCode() != null ? request.invoiceCode() : "NA");
        // Consonancia con el Swing (FacturaCompraDao): ACT = activa, NULA = anulada.
        header.setEstadoFactura("ACT");
        header.setIsv18(BigDecimal.ZERO);
        header.setUsuario(principal.getName());
        header.setPago(request.paymentAmount() != null ? request.paymentAmount() : BigDecimal.ZERO);
        header.setNoFacturaCompra(request.supplierInvoiceNumber());
        header.setFechaIngreso(LocalDate.now());
        header.setTipoFactura(request.invoiceType());
        header.setFechaVencimiento(request.dueDate() != null ? request.dueDate() : fecha);
        header.setAgregaKardex(0);
        header.setCodigoBodega(request.warehouseCode());

        // 4) Persistir header → obtiene numero_compra
        EncabezadoFacturaCompra savedHeader = headerCrud.save(header);
        Integer numeroCompra = savedHeader.getNumeroCompra();

        // 5) Persistir lineas. Cada save dispara detalle_compra_b_inset →
        //    crear_compa_kardex (stock sube + existencia_articulo_bodega upserted).
        List<DetalleFacturaCompra> savedLines = new ArrayList<>(request.lines().size());
        for (PurchaseLineRequest l : request.lines()) {
            DetalleFacturaCompra detail = new DetalleFacturaCompra();
            detail.setNumeroCompra(numeroCompra);
            detail.setCodigoArticulo(l.productId());
            detail.setPrecio(l.price());
            detail.setCantidad(l.quantity());
            detail.setImpuesto(l.tax() != null ? l.tax() : BigDecimal.ZERO);
            detail.setSubtotal(l.price().multiply(l.quantity()));
            detail.setAgregaKardex(0);   // el trigger lo pone a 1 al insertar
            detail.setCodigoBodega(request.warehouseCode());
            detail.setFechaVenc(l.expirationDate());
            savedLines.add(lineCrud.save(detail));
        }
        savedHeader.setLineas(savedLines);

        // Hidratar manualmente el @ManyToOne proveedor (session cache no lo
        // recarga tras save). Asi supplierName aparece en la POST response.
        supplierCrud.findById(request.supplierId()).ifPresent(savedHeader::setProveedor);
        return mapper.toResponse(savedHeader);
    }

    public Page<PurchaseResponse> search(Integer supplier, LocalDate from, LocalDate to,
                                         int page, int size) {
        return headerCrud.search(supplier, from, to, PageRequest.of(page, size))
                .map(mapper::toResponse);
    }

    public PurchaseResponse getById(int id) {
        EncabezadoFacturaCompra header = headerCrud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase " + id + " not found"));
        // forzar carga de lineas (lazy) — al estar dentro del @Transactional
        // implicito del request, JPA puede hacerlo aunque el getter sea LAZY.
        List<DetalleFacturaCompra> lineas = lineCrud.findByNumeroCompra(id);
        header.setLineas(lineas);
        return withProductNames(mapper.toResponse(header), lineas);
    }

    /**
     * Anula TOTALMENTE una factura de compra (mirror de CtlFacturasCompra del
     * Swing): valida la clave de supervisor, devuelve todas las líneas al kardex
     * (cada insert en detalle_devoluciones_compra dispara crear_dev_compa_kardex,
     * que BAJA el stock que la compra subió) y marca la compra NULA.
     */
    @Transactional
    public AnnulPurchaseResponse annul(int purchaseId, AnnulPurchaseRequest req, Principal principal) {
        if (!adminAuth.verifyAdminPassword(req.supervisorPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave de supervisor inválida");
        }
        EncabezadoFacturaCompra header = headerCrud.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase " + purchaseId + " not found"));
        if ("NULA".equalsIgnoreCase(header.getEstadoFactura())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra ya está anulada");
        }

        // Devolver todas las líneas → trigger baja el stock que la compra subió.
        List<DetalleFacturaCompra> lineas = lineCrud.findByNumeroCompra(purchaseId);
        LocalDate hoy = LocalDate.now();
        BigDecimal totalDevuelto = BigDecimal.ZERO;
        for (DetalleFacturaCompra l : lineas) {
            DetalleDevolucionCompra d = new DetalleDevolucionCompra();
            d.setNumeroFactura(purchaseId);
            d.setCodigoArticulo(l.getCodigoArticulo());
            d.setPrecio(l.getPrecio());
            d.setCantidad(l.getCantidad());
            d.setImpuesto(l.getImpuesto() != null ? l.getImpuesto() : BigDecimal.ZERO);
            d.setDescuento(BigDecimal.ZERO);
            BigDecimal sub = l.getPrecio().multiply(l.getCantidad());
            d.setSubtotal(sub);
            d.setTotal(sub.add(d.getImpuesto()));
            d.setFecha(hoy);
            d.setAgregaKardex(0);   // el trigger lo pone a 1
            d.setCodigoBodega(header.getCodigoBodega());
            returnCrud.save(d);
            totalDevuelto = totalDevuelto.add(d.getTotal());
        }

        header.setEstadoFactura("NULA");
        headerCrud.save(header);

        boolean credito = header.getTipoFactura() != null && header.getTipoFactura() == 2;
        // TODO(CxP): si la compra es a crédito, reversar la cuenta por pagar del
        //   proveedor (mirror de createDebitoToProveedor del Swing: débito en
        //   cuentas_por_pagar / recibo_pago_proveedores). Pendiente — por ahora
        //   solo se reversa el kardex y se marca NULA.
        log.info("Anulación compra {} user {} motivo='{}' devuelto={} credito={}",
                purchaseId, principal != null ? principal.getName() : "SYSTEM",
                req.motivo(), totalDevuelto, credito);
        return new AnnulPurchaseResponse(purchaseId, "NULA", totalDevuelto, false, BigDecimal.ZERO);
    }

    /**
     * Enriquece las líneas de la respuesta con el nombre del artículo (la
     * entidad de detalle solo guarda el código). Batch lookup para el drawer
     * de detalle del POS; el record es inmutable, así que se reconstruye.
     */
    private PurchaseResponse withProductNames(PurchaseResponse resp, List<DetalleFacturaCompra> lineas) {
        List<Integer> ids = lineas.stream().map(DetalleFacturaCompra::getCodigoArticulo).distinct().toList();
        Map<Integer, String> names = productCrud.findAllById(ids).stream()
                .collect(Collectors.toMap(a -> a.getCodigoArticulo(), a -> a.getArticulo(), (a, b) -> a));
        List<PurchaseLineResponse> enriched = resp.lines().stream()
                .map(l -> new PurchaseLineResponse(
                        l.id(), l.productId(),
                        names.getOrDefault(l.productId(), "Art. " + l.productId()),
                        l.quantity(), l.price(), l.tax(), l.subtotal(), l.expirationDate()))
                .toList();
        return new PurchaseResponse(
                resp.id(), resp.supplierId(), resp.supplierName(), resp.supplierInvoiceNumber(),
                resp.warehouseCode(), resp.date(), resp.status(), resp.invoiceType(), resp.dueDate(),
                resp.subtotal(), resp.tax(), resp.total(), resp.payment(), resp.user(), enriched);
    }
}

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PurchaseLineRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleFacturaCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.EncabezadoFacturaCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.ProveedorCRUD;
import net.datatecsolution.admintools.persistence.entity.DetalleFacturaCompra;
import net.datatecsolution.admintools.persistence.entity.EncabezadoFacturaCompra;
import net.datatecsolution.admintools.persistence.mapper.PurchaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea y consulta compras (INV-5). El kardex y el balance materializado
 * los maneja la BD via trigger en {@code detalle_factura_compra} — este
 * service solo inserta los documentos en una transaccion.
 */
@Service
public class PurchaseService {

    @Autowired private EncabezadoFacturaCompraCRUD headerCrud;
    @Autowired private DetalleFacturaCompraCRUD lineCrud;
    @Autowired private ProveedorCRUD supplierCrud;
    @Autowired private BodegaCRUD warehouseCrud;
    @Autowired private ArticuloMasterCRUD productCrud;
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
        header.setEstadoFactura("Activa");
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
        header.setLineas(lineCrud.findByNumeroCompra(id));
        return mapper.toResponse(header);
    }
}

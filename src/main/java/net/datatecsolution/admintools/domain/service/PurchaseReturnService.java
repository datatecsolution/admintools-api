package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnLineRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnLineResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnRequest;
import net.datatecsolution.admintools.domain.dto.PurchaseReturnResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleDevolucionCompraCRUD;
import net.datatecsolution.admintools.persistence.crud.EncabezadoFacturaCompraCRUD;
import net.datatecsolution.admintools.persistence.entity.DetalleDevolucionCompra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Devoluciones de compra al proveedor (INV-7). El kardex y el balance
 * materializado se actualizan via el trigger
 * {@code detalle_devolucion_compra_b_i} → {@code crear_dev_compa_kardex}.
 * El API solo inserta los items en una transaccion.
 */
@Service
public class PurchaseReturnService {

    @Autowired private DetalleDevolucionCompraCRUD crud;
    @Autowired private EncabezadoFacturaCompraCRUD purchaseCrud;
    @Autowired private BodegaCRUD warehouseCrud;
    @Autowired private ArticuloMasterCRUD productCrud;

    @Transactional
    public PurchaseReturnResponse create(PurchaseReturnRequest request) {
        // (1) Validar FKs
        if (!purchaseCrud.existsById(request.purchaseId())) {
            throw new EntityNotFoundException("Purchase " + request.purchaseId() + " not found");
        }
        if (!warehouseCrud.existsById(request.warehouseCode())) {
            throw new EntityNotFoundException("Warehouse " + request.warehouseCode() + " not found");
        }
        for (PurchaseReturnLineRequest item : request.items()) {
            if (!productCrud.existsById(item.productId())) {
                throw new EntityNotFoundException("Product " + item.productId() + " not found");
            }
        }

        LocalDate fecha = request.date() != null ? request.date() : LocalDate.now();

        // (2) Insertar items. Cada save dispara el trigger →
        //     crear_dev_compa_kardex → stock baja + existencia_articulo_bodega upsert.
        List<DetalleDevolucionCompra> saved = new ArrayList<>(request.items().size());
        BigDecimal totalReturned = BigDecimal.ZERO;
        for (PurchaseReturnLineRequest item : request.items()) {
            DetalleDevolucionCompra d = new DetalleDevolucionCompra();
            d.setNumeroFactura(request.purchaseId());
            d.setCodigoArticulo(item.productId());
            d.setPrecio(item.price());
            d.setCantidad(item.quantity());
            d.setImpuesto(item.tax() != null ? item.tax() : BigDecimal.ZERO);
            d.setDescuento(item.discount() != null ? item.discount() : BigDecimal.ZERO);
            BigDecimal lineSubtotal = item.price().multiply(item.quantity());
            d.setSubtotal(lineSubtotal);
            BigDecimal lineTotal = lineSubtotal
                    .add(d.getImpuesto())
                    .subtract(d.getDescuento());
            d.setTotal(lineTotal);
            d.setFecha(fecha);
            d.setAgregaKardex(0);   // trigger lo pone a 1
            d.setCodigoBodega(request.warehouseCode());
            saved.add(crud.save(d));
            totalReturned = totalReturned.add(lineTotal);
        }

        return buildResponse(request.purchaseId(), request.warehouseCode(), fecha,
                totalReturned, saved);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseReturnLineResponse> search(Integer purchase, LocalDate from, LocalDate to,
                                                    int page, int size) {
        return crud.search(purchase, from, to, PageRequest.of(page, size)).map(this::toLineResponse);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturnLineResponse> getByPurchase(int purchaseId) {
        return crud.findByNumeroFactura(purchaseId).stream().map(this::toLineResponse).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseReturnLineResponse getById(int id) {
        return crud.findById(id).map(this::toLineResponse)
                .orElseThrow(() -> new EntityNotFoundException("PurchaseReturn " + id + " not found"));
    }

    // ---- helpers ----

    private PurchaseReturnResponse buildResponse(int purchaseId, int warehouseCode, LocalDate date,
                                                  BigDecimal totalReturned,
                                                  List<DetalleDevolucionCompra> rows) {
        List<PurchaseReturnLineResponse> items = new ArrayList<>(rows.size());
        for (DetalleDevolucionCompra d : rows) items.add(toLineResponse(d));
        return new PurchaseReturnResponse(purchaseId, warehouseCode, date, totalReturned, items);
    }

    private PurchaseReturnLineResponse toLineResponse(DetalleDevolucionCompra d) {
        return new PurchaseReturnLineResponse(
                d.getCodigoDevolucion(),
                d.getCodigoArticulo(),
                d.getCantidad(),
                d.getPrecio(),
                d.getImpuesto(),
                d.getDescuento(),
                d.getSubtotal(),
                d.getTotal());
    }
}

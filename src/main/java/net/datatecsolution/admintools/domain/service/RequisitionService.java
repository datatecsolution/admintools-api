package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.RequisitionLineRequest;
import net.datatecsolution.admintools.domain.dto.RequisitionLineResponse;
import net.datatecsolution.admintools.domain.dto.RequisitionRequest;
import net.datatecsolution.admintools.domain.dto.RequisitionResponse;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.DetalleRequisicionCRUD;
import net.datatecsolution.admintools.persistence.crud.EncabezadoRequisicionCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.DetalleRequisicion;
import net.datatecsolution.admintools.persistence.entity.EncabezadoRequisicion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea y consulta requisiciones (INV-6). Caso comun: merma del bodega
 * de trabajo a "Perdidas". El kardex de ambas bodegas y el balance
 * materializado los maneja el trigger {@code d_requisicion_b_insert}
 * via los SPs crear_requisicion_entrada/salida_kardex — el API NO toca
 * el kardex.
 */
@Service
public class RequisitionService {

    @Autowired private EncabezadoRequisicionCRUD headerCrud;
    @Autowired private DetalleRequisicionCRUD lineCrud;
    @Autowired private BodegaCRUD warehouseCrud;
    @Autowired private ArticuloMasterCRUD productCrud;

    @Transactional
    public RequisitionResponse create(RequisitionRequest request, Principal principal) {
        // (1) Validar bodegas distintas
        if (request.originWarehouseCode().equals(request.destinationWarehouseCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "originWarehouseCode y destinationWarehouseCode deben ser distintos");
        }

        // (2) Validar FKs
        Bodega origin = warehouseCrud.findById(request.originWarehouseCode())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse " + request.originWarehouseCode() + " not found"));
        Bodega destination = warehouseCrud.findById(request.destinationWarehouseCode())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse " + request.destinationWarehouseCode() + " not found"));
        for (RequisitionLineRequest line : request.lines()) {
            if (!productCrud.existsById(line.productId())) {
                throw new EntityNotFoundException("Product " + line.productId() + " not found");
            }
        }

        // (3) Calcular total
        BigDecimal totalSum = BigDecimal.ZERO;
        for (RequisitionLineRequest line : request.lines()) {
            totalSum = totalSum.add(line.unitPrice().multiply(line.quantity()));
        }

        // (4) Construir encabezado
        EncabezadoRequisicion header = new EncabezadoRequisicion();
        header.setFecha(request.date() != null ? request.date() : LocalDateTime.now());
        header.setTotal(totalSum);
        header.setCodigoDepartOrigen(request.originWarehouseCode());
        header.setCodigoDepartDestino(request.destinationWarehouseCode());
        header.setUsuario(principal.getName());
        header.setAgregaKardex(0);
        header.setEstadoRequisicion("ACT");
        EncabezadoRequisicion saved = headerCrud.save(header);

        // (5) Insertar lineas. Cada save dispara d_requisicion_b_insert →
        //     SP salida (origen) + SP entrada (destino) → existencia_articulo_bodega
        //     se actualiza para ambas bodegas en la misma transaccion.
        List<DetalleRequisicion> savedLines = new ArrayList<>(request.lines().size());
        for (RequisitionLineRequest l : request.lines()) {
            DetalleRequisicion d = new DetalleRequisicion();
            d.setCodigoRequisicion(saved.getCodigoRequisicion());
            d.setCodigoArticulo(l.productId());
            d.setCantidad(l.quantity());
            d.setPrecioUnidad(l.unitPrice());
            d.setTotal(l.unitPrice().multiply(l.quantity()));
            d.setCodigoDepartOrigen(request.originWarehouseCode());
            d.setCodigoDepartDestino(request.destinationWarehouseCode());
            d.setAgregaKardex(0);   // trigger lo pone a 1
            savedLines.add(lineCrud.save(d));
        }

        return toResponse(saved, origin, destination, savedLines);
    }

    @Transactional(readOnly = true)
    public Page<RequisitionResponse> search(Integer origin, Integer destination,
                                            LocalDateTime from, LocalDateTime to,
                                            int page, int size) {
        return headerCrud.search(origin, destination, from, to, PageRequest.of(page, size))
                .map(this::toResponseWithLookup);
    }

    @Transactional(readOnly = true)
    public RequisitionResponse getById(int id) {
        EncabezadoRequisicion header = headerCrud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Requisition " + id + " not found"));
        return toResponseWithLookup(header);
    }

    // ---- helpers ----

    /** Carga bodegas + lineas y arma la response. Para search/getById (no tenemos referencias). */
    private RequisitionResponse toResponseWithLookup(EncabezadoRequisicion header) {
        Bodega origin = warehouseCrud.findById(header.getCodigoDepartOrigen()).orElse(null);
        Bodega destination = warehouseCrud.findById(header.getCodigoDepartDestino()).orElse(null);
        List<DetalleRequisicion> lines = lineCrud.findByCodigoRequisicion(header.getCodigoRequisicion());
        return toResponse(header, origin, destination, lines);
    }

    private RequisitionResponse toResponse(EncabezadoRequisicion h, Bodega origin, Bodega destination,
                                            List<DetalleRequisicion> lines) {
        List<RequisitionLineResponse> lineDtos = new ArrayList<>(lines.size());
        for (DetalleRequisicion d : lines) {
            lineDtos.add(new RequisitionLineResponse(
                    d.getIdDetalleRequisicion(),
                    d.getCodigoArticulo(),
                    d.getCantidad(),
                    d.getPrecioUnidad(),
                    d.getTotal()));
        }
        return new RequisitionResponse(
                h.getCodigoRequisicion(),
                h.getCodigoDepartOrigen(),
                origin != null ? origin.getDescripcionBodega() : null,
                h.getCodigoDepartDestino(),
                destination != null ? destination.getDescripcionBodega() : null,
                h.getFecha(),
                h.getEstadoRequisicion(),
                h.getTotal(),
                h.getUsuario(),
                lineDtos);
    }
}

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.InventoryCountLineRequest;
import net.datatecsolution.admintools.domain.dto.InventoryCountLineResponse;
import net.datatecsolution.admintools.domain.dto.InventoryCountRequest;
import net.datatecsolution.admintools.domain.dto.InventoryCountResponse;
import net.datatecsolution.admintools.persistence.crud.InventoryCountCRUD;
import net.datatecsolution.admintools.persistence.crud.InventoryCountLineCRUD;
import net.datatecsolution.admintools.persistence.entity.InventoryCount;
import net.datatecsolution.admintools.persistence.entity.InventoryCountLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra y consulta actas de toma física (gap #2 del módulo de inventario).
 * Persiste el conteo en sí (encabezado + detalle), aparte de los movimientos
 * del cierre (requisición/compras). El encabezado (conteos + valores) lo DERIVA
 * el service a partir de las líneas, con la misma clasificación que el front
 * (store.ts:lineStatus): negativo si sistema<0, si no faltante/sobrante/ok.
 */
@Service
public class InventoryCountService {

    @Autowired private InventoryCountCRUD headerCrud;
    @Autowired private InventoryCountLineCRUD lineCrud;

    @Transactional
    public InventoryCountResponse create(InventoryCountRequest request, Principal principal) {
        InventoryCount header = new InventoryCount();
        header.setFecha(request.date() != null ? request.date() : LocalDateTime.now());
        header.setUsuario(principal != null ? principal.getName() : "system");
        header.setCodigoBodega(request.warehouseCode());
        header.setMotivo(request.motivo());
        header.setEstado("ACT");

        int contadas = 0, faltantes = 0, sobrantes = 0, negativos = 0;
        BigDecimal valorAjuste = BigDecimal.ZERO;
        BigDecimal valorNegativos = BigDecimal.ZERO;

        List<InventoryCountLine> lines = new ArrayList<>(request.lines().size());
        for (InventoryCountLineRequest l : request.lines()) {
            BigDecimal sistema = l.sistema();
            BigDecimal fisico = l.fisico();
            BigDecimal costo = l.costo() != null ? l.costo() : BigDecimal.ZERO;
            BigDecimal diferencia = fisico.subtract(sistema);
            String estado = classify(sistema, diferencia);

            contadas++;
            switch (estado) {
                case "negativo" -> {
                    negativos++;
                    valorNegativos = valorNegativos.add(sistema.abs().multiply(costo));
                }
                case "faltante" -> {
                    faltantes++;
                    valorAjuste = valorAjuste.add(diferencia.abs().multiply(costo));
                }
                case "sobrante" -> {
                    sobrantes++;
                    valorAjuste = valorAjuste.add(diferencia.abs().multiply(costo));
                }
                default -> { /* ok: sin movimiento ni valor */ }
            }

            InventoryCountLine line = new InventoryCountLine();
            line.setCodigoArticulo(l.productId());
            line.setSistema(sistema);
            line.setFisico(fisico);
            line.setDiferencia(diferencia);
            line.setCosto(costo);
            line.setEstadoLinea(estado);
            lines.add(line);
        }

        header.setContadas(contadas);
        header.setFaltantes(faltantes);
        header.setSobrantes(sobrantes);
        header.setNegativos(negativos);
        header.setValorAjuste(valorAjuste);
        header.setValorNegativos(valorNegativos);
        InventoryCount saved = headerCrud.save(header);

        List<InventoryCountLine> savedLines = new ArrayList<>(lines.size());
        for (InventoryCountLine line : lines) {
            line.setCodigoInventarioCount(saved.getCodigoInventarioCount());
            savedLines.add(lineCrud.save(line));
        }

        return toResponse(saved, savedLines);
    }

    @Transactional(readOnly = true)
    public Page<InventoryCountResponse> search(Integer warehouse, LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        return headerCrud.search(warehouse, from, to, PageRequest.of(page, size))
                .map(h -> toResponse(h, lineCrud.findByCodigoInventarioCount(h.getCodigoInventarioCount())));
    }

    @Transactional(readOnly = true)
    public InventoryCountResponse getById(int id) {
        InventoryCount header = headerCrud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("InventoryCount " + id + " not found"));
        return toResponse(header, lineCrud.findByCodigoInventarioCount(id));
    }

    /** Misma clasificación que el front: negativo si sistema<0, si no por la diferencia. */
    private String classify(BigDecimal sistema, BigDecimal diferencia) {
        if (sistema.signum() < 0) return "negativo";
        int c = diferencia.signum();
        return c < 0 ? "faltante" : c > 0 ? "sobrante" : "ok";
    }

    private InventoryCountResponse toResponse(InventoryCount h, List<InventoryCountLine> lines) {
        List<InventoryCountLineResponse> lineDtos = new ArrayList<>(lines.size());
        for (InventoryCountLine l : lines) {
            lineDtos.add(new InventoryCountLineResponse(
                    l.getIdDetalleInventarioCount(),
                    l.getCodigoArticulo(),
                    l.getSistema(),
                    l.getFisico(),
                    l.getDiferencia(),
                    l.getCosto(),
                    l.getEstadoLinea()));
        }
        return new InventoryCountResponse(
                h.getCodigoInventarioCount(),
                h.getFecha(),
                h.getUsuario(),
                h.getCodigoBodega(),
                h.getContadas(),
                h.getFaltantes(),
                h.getSobrantes(),
                h.getNegativos(),
                h.getValorAjuste(),
                h.getValorNegativos(),
                h.getMotivo(),
                h.getEstado(),
                lineDtos);
    }
}

package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PaymentMethodRequest;
import net.datatecsolution.admintools.domain.dto.PaymentMethodResponse;
import net.datatecsolution.admintools.persistence.crud.TipoPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.TipoPago;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * US-032 — CRUD del catalogo de metodos de pago (tabla tipo_pago).
 */
@Service
public class PaymentMethodService {

    private final TipoPagoCRUD crud;

    public PaymentMethodService(TipoPagoCRUD crud) {
        this.crud = crud;
    }

    public List<PaymentMethodResponse> getAll() {
        return crud.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(PaymentMethodService::toResponse)
                .toList();
    }

    public PaymentMethodResponse create(PaymentMethodRequest req) {
        TipoPago t = new TipoPago();
        t.setDescripcion(req.descripcion().trim());
        return toResponse(crud.save(t));
    }

    public PaymentMethodResponse update(int id, PaymentMethodRequest req) {
        TipoPago t = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Metodo de pago " + id + " no encontrado"));
        t.setDescripcion(req.descripcion().trim());
        return toResponse(crud.save(t));
    }

    public void delete(int id) {
        if (!crud.existsById(id)) {
            throw new EntityNotFoundException("Metodo de pago " + id + " no encontrado");
        }
        crud.deleteById(id);
    }

    private static PaymentMethodResponse toResponse(TipoPago t) {
        return new PaymentMethodResponse(t.getId(), t.getDescripcion());
    }
}

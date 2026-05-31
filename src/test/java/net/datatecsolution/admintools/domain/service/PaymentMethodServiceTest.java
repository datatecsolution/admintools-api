package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.PaymentMethodRequest;
import net.datatecsolution.admintools.domain.dto.PaymentMethodResponse;
import net.datatecsolution.admintools.persistence.crud.TipoPagoCRUD;
import net.datatecsolution.admintools.persistence.entity.TipoPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock private TipoPagoCRUD crud;

    private PaymentMethodService service() {
        return new PaymentMethodService(crud);
    }

    private TipoPago tp(int id, String desc) {
        TipoPago t = new TipoPago();
        t.setId(id);
        t.setDescripcion(desc);
        return t;
    }

    @Test
    void create_guardaYDevuelve() {
        when(crud.save(any(TipoPago.class))).thenAnswer(inv -> {
            TipoPago t = inv.getArgument(0);
            t.setId(3);
            return t;
        });

        PaymentMethodResponse r = service().create(new PaymentMethodRequest("  Tarjeta  "));

        assertThat(r.id()).isEqualTo(3);
        assertThat(r.descripcion()).isEqualTo("Tarjeta"); // trim
    }

    @Test
    void update_inexistente_lanza404() {
        when(crud.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(99, new PaymentMethodRequest("X")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_inexistente_lanza404() {
        when(crud.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service().delete(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existente_borra() {
        when(crud.existsById(3)).thenReturn(true);

        service().delete(3);

        verify(crud).deleteById(3);
    }

    @Test
    void getAll_ordenaPorId() {
        when(crud.findAll()).thenReturn(List.of(tp(2, "Tarjeta"), tp(1, "Efectivo")));

        List<PaymentMethodResponse> r = service().getAll();

        assertThat(r).extracting(PaymentMethodResponse::id).containsExactly(1, 2);
    }
}

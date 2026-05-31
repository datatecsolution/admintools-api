package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.TaxRequest;
import net.datatecsolution.admintools.domain.dto.TaxResponse;
import net.datatecsolution.admintools.persistence.crud.ImpuestoCRUD;
import net.datatecsolution.admintools.persistence.entity.Impuesto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCatalogServiceTest {

    @Mock private ImpuestoCRUD crud;

    private TaxCatalogService service() {
        return new TaxCatalogService(crud);
    }

    @Test
    void create_guardaPorcentajeComoString() {
        when(crud.save(any(Impuesto.class))).thenAnswer(inv -> {
            Impuesto i = inv.getArgument(0);
            i.setId(4);
            return i;
        });

        TaxResponse r = service().create(new TaxRequest("ISV 15%", new BigDecimal("15")));

        ArgumentCaptor<Impuesto> cap = ArgumentCaptor.forClass(Impuesto.class);
        verify(crud).save(cap.capture());
        assertThat(cap.getValue().getPorcentaje()).isEqualTo("15");
        assertThat(cap.getValue().getDescripcion()).isEqualTo("ISV 15%");
        assertThat(r.id()).isEqualTo(4);
        assertThat(r.percent()).isEqualByComparingTo("15");
    }

    @Test
    void update_inexistente_lanza404() {
        when(crud.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(99, new TaxRequest("X", BigDecimal.ZERO)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_inexistente_lanza404() {
        when(crud.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service().delete(99))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

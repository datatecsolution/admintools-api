package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CompanyRequest;
import net.datatecsolution.admintools.domain.dto.CompanyResponse;
import net.datatecsolution.admintools.persistence.crud.DatosEmpresaCRUD;
import net.datatecsolution.admintools.persistence.entity.DatosEmpresa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private DatosEmpresaCRUD crud;

    private CompanyService service() {
        return new CompanyService(crud);
    }

    @Test
    void getCompany_sinFila_devuelveVacio() {
        when(crud.findAll()).thenReturn(List.of());

        CompanyResponse r = service().getCompany();

        assertThat(r.id()).isNull();
        assertThat(r.nombre()).isEmpty();
    }

    @Test
    void updateCompany_creaCuandoNoExiste_yCoalesceNulls() {
        when(crud.findAll()).thenReturn(List.of());
        when(crud.save(any(DatosEmpresa.class))).thenAnswer(inv -> {
            DatosEmpresa e = inv.getArgument(0);
            e.setId(1);
            return e;
        });

        CompanyResponse r = service().updateCompany(new CompanyRequest(
                "Mi Negocio", "08011985123456", null, null, null, null, "http://x/logo.png"));

        ArgumentCaptor<DatosEmpresa> cap = ArgumentCaptor.forClass(DatosEmpresa.class);
        verify(crud).save(cap.capture());
        assertThat(cap.getValue().getNombre()).isEqualTo("Mi Negocio");
        assertThat(cap.getValue().getTelefono()).isEmpty(); // null -> "" (columna NOT NULL)
        assertThat(cap.getValue().getLogoUrl()).isEqualTo("http://x/logo.png");
        assertThat(r.id()).isEqualTo(1);
    }

    @Test
    void updateCompany_actualizaFilaExistente() {
        DatosEmpresa existente = new DatosEmpresa();
        existente.setId(7);
        existente.setNombre("Viejo");
        when(crud.findAll()).thenReturn(List.of(existente));
        when(crud.save(any(DatosEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyResponse r = service().updateCompany(new CompanyRequest(
                "Nuevo", "", "9999-9999", "a@b.com", "Dueno", "Centro", null));

        assertThat(r.id()).isEqualTo(7); // misma fila
        assertThat(r.nombre()).isEqualTo("Nuevo");
        assertThat(r.telefono()).isEqualTo("9999-9999");
    }
}

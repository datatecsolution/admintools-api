package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CajaRequest;
import net.datatecsolution.admintools.domain.dto.CajaResponse;
import net.datatecsolution.admintools.persistence.crud.BodegaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.entity.Bodega;
import net.datatecsolution.admintools.persistence.entity.Caja;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-101 — provisioning de cajas: replica de CajaDao.registrar del Swing
 * (fila → nombre_db derivado → CREATE DATABASE → migrar → tenant) más la
 * compensación que el Swing no tiene.
 */
@ExtendWith(MockitoExtension.class)
class CajaAdminServiceTest {

    @Mock private CajaCRUD cajaCRUD;
    @Mock private BodegaCRUD bodegaCRUD;
    @Mock private CajaProvisioner provisioner;

    private CajaAdminService service() {
        return new CajaAdminService(cajaCRUD, bodegaCRUD, provisioner);
    }

    private void stubSaveAssignsId(int codigo) {
        when(cajaCRUD.save(any(Caja.class))).thenAnswer(inv -> {
            Caja c = inv.getArgument(0);
            if (c.getCodigo() == null) c.setCodigo(codigo);
            return c;
        });
    }

    @Test
    void create_derivaNombreDbYProvisionaEnOrden() {
        when(bodegaCRUD.findById(3)).thenReturn(Optional.of(new Bodega(3, "Bodega Norte")));
        stubSaveAssignsId(9);

        CajaResponse resp = service().create(new CajaRequest("Caja 9", 3));

        assertThat(resp.id()).isEqualTo(9);
        assertThat(resp.dbName()).isEqualTo("admin_tools_caja_9");
        assertThat(resp.warehouseName()).isEqualTo("Bodega Norte");

        InOrder inOrder = inOrder(provisioner);
        inOrder.verify(provisioner).createDatabase("admin_tools_caja_9");
        inOrder.verify(provisioner).migrate("admin_tools_caja_9", 3);
        inOrder.verify(provisioner).registerTenant("admin_tools_caja_9");
    }

    @Test
    void create_bodegaInexistente_422SinTocarNada() {
        when(bodegaCRUD.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(new CajaRequest("Caja X", 99)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(cajaCRUD, never()).save(any());
        verify(provisioner, never()).createDatabase(any());
    }

    @Test
    void create_migracionFalla_compensaDropYDeleteFila() {
        when(bodegaCRUD.findById(3)).thenReturn(Optional.of(new Bodega(3, "Bodega Norte")));
        stubSaveAssignsId(9);
        doThrow(new IllegalStateException("boom")).when(provisioner).migrate("admin_tools_caja_9", 3);

        assertThatThrownBy(() -> service().create(new CajaRequest("Caja 9", 3)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(provisioner).dropDatabase("admin_tools_caja_9");
        verify(cajaCRUD).deleteById(9);
        verify(provisioner, never()).registerTenant(any());
    }

    @Test
    void create_createDatabaseFalla_borraFilaSinDrop() {
        when(bodegaCRUD.findById(3)).thenReturn(Optional.of(new Bodega(3, "Bodega Norte")));
        stubSaveAssignsId(9);
        doThrow(new IllegalStateException("sin privilegios")).when(provisioner)
                .createDatabase("admin_tools_caja_9");

        assertThatThrownBy(() -> service().create(new CajaRequest("Caja 9", 3)))
                .isInstanceOf(ResponseStatusException.class);

        verify(provisioner, never()).dropDatabase(any());
        verify(cajaCRUD).deleteById(9);
    }

    @Test
    void update_actualizaDescripcionYBodega_sinProvisioning() {
        Caja existente = new Caja();
        existente.setCodigo(2);
        existente.setDescripcion("Vieja");
        existente.setCodigoBodega(1);
        existente.setNombreDb("admin_tools_caja_2");
        when(cajaCRUD.findById(2)).thenReturn(Optional.of(existente));
        when(bodegaCRUD.findById(3)).thenReturn(Optional.of(new Bodega(3, "Bodega Norte")));
        when(cajaCRUD.save(any(Caja.class))).thenAnswer(inv -> inv.getArgument(0));

        CajaResponse resp = service().update(2, new CajaRequest("Caja renombrada", 3));

        assertThat(resp.name()).isEqualTo("Caja renombrada");
        assertThat(resp.warehouseId()).isEqualTo(3);
        assertThat(resp.dbName()).isEqualTo("admin_tools_caja_2");
        verify(provisioner, never()).createDatabase(any());
    }
}

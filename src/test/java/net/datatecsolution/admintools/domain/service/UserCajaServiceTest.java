package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.UserCajaUpsertRequest;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-104 — reglas de asignación de cajas: un CAJERO (tipoPermiso 2) debe
 * tener entre 1 y 2 cajas; otros tipos sin límite. Si el set final no tiene
 * exactamente 2 cajas, el flag de rotación automática se apaga (auto-apagado).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCajaServiceTest {

    private static final int USER_ID = 7;
    private static final String USERNAME = "tecnico";

    @Mock private CajaUsuarioCRUD crud;
    @Mock private UsuarioCRUD usuariosCrud;
    @Mock private CajaCRUD cajasCrud;
    @Mock private ConfigUserFacturacionCRUD configCrud;

    private UserCajaService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new UserCajaService(crud, usuariosCrud, cajasCrud, configCrud);

        usuario = new Usuario();
        usuario.setIdUsuario((long) USER_ID);
        usuario.setNombreUsuario(USERNAME);
        usuario.setTipoPermiso(2); // cajero por defecto en los tests
        when(usuariosCrud.findById(USER_ID)).thenReturn(Optional.of(usuario));

        when(cajasCrud.findAll()).thenReturn(List.of(
                caja(1, "Prueba"), caja(2, "CAJA_SAR"), caja(3, "Caja 3"),
                caja(4, "Caja 4"), caja(5, "Caja 5")));
        when(crud.findByIdUsuario(USERNAME)).thenReturn(List.of());
    }

    private static Caja caja(int codigo, String descripcion) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setDescripcion(descripcion);
        return c;
    }

    private static UserCajaUpsertRequest fila(int cajaId, boolean def) {
        return new UserCajaUpsertRequest(cajaId, def);
    }

    // ---------- límite 1..2 para cajeros ----------

    @Test
    void cajeroConCeroCajas_es400() {
        assertThatThrownBy(() -> service.replaceAll(USER_ID, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("entre 1 y 2 cajas");
        verify(crud, never()).deleteByIdUsuario(anyString());
    }

    @Test
    void cajeroConTresCajas_es400() {
        assertThatThrownBy(() -> service.replaceAll(USER_ID,
                List.of(fila(1, true), fila(2, false), fila(3, false))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("entre 1 y 2 cajas");
        verify(crud, never()).saveAll(anyList());
    }

    @Test
    void cajeroConUnaCaja_ok_yApagaRotacion() {
        service.replaceAll(USER_ID, List.of(fila(1, true)));

        // con != 2 cajas la rotación automática no puede seguir activa
        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 0);
        assertThat(usuario.getCodigoCaja()).isEqualTo(1); // sync default
    }

    @Test
    void cajeroConDosCajas_ok_yNoTocaRotacion() {
        service.replaceAll(USER_ID, List.of(fila(1, true), fila(2, false)));

        verify(configCrud, never()).updateRotacionAutomaticaCajas(anyString(), anyInt());
        assertThat(usuario.getCodigoCaja()).isEqualTo(1);
    }

    // ---------- otros tipos sin límite ----------

    @Test
    void adminSinCajas_ok_yApagaRotacion() {
        usuario.setTipoPermiso(4);

        assertThat(service.replaceAll(USER_ID, List.of())).isEmpty();

        // branch vacío: apaga el flag (ex-cajero promovido con flag colgado)
        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 0);
        assertThat(usuario.getCodigoCaja()).isZero();
    }

    @Test
    void adminConCincoCajas_ok() {
        usuario.setTipoPermiso(4);

        service.replaceAll(USER_ID, List.of(
                fila(1, true), fila(2, false), fila(3, false), fila(4, false), fila(5, false)));

        // 5 != 2 → auto-apagado también aplica (inofensivo para no-cajeros)
        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 0);
    }

    // ---------- regresiones de las validaciones preexistentes ----------

    @Test
    void cajaIdDuplicado_es400() {
        assertThatThrownBy(() -> service.replaceAll(USER_ID, List.of(fila(1, true), fila(1, false))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    void masDeUnDefault_es400() {
        assertThatThrownBy(() -> service.replaceAll(USER_ID, List.of(fila(1, true), fila(2, true))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exactamente 1 caja");
    }

    @Test
    void cajaInexistente_es400() {
        assertThatThrownBy(() -> service.replaceAll(USER_ID, List.of(fila(99, true))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no existe");
    }
}

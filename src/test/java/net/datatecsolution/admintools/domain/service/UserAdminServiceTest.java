package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.UserResponse;
import net.datatecsolution.admintools.domain.dto.UserUpdateRequest;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.crud.UsuarioCRUD;
import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import net.datatecsolution.admintools.persistence.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-104 — setRotacion (encender exige cajero + exactamente 2 cajas + fila de
 * config existente) y el auto-apagado del flag cuando update() cambia el tipo
 * a no-cajero. También el mapeo del flag en getAll/getById.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAdminServiceTest {

    private static final int USER_ID = 7;
    private static final String USERNAME = "tecnico";

    @Mock private UsuarioCRUD crud;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CajaUsuarioCRUD cajaUsuarioCrud;
    @Mock private ConfigUserFacturacionCRUD configCrud;

    private UserAdminService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(crud, passwordEncoder, cajaUsuarioCrud, configCrud);

        usuario = new Usuario();
        usuario.setIdUsuario((long) USER_ID);
        usuario.setNombreUsuario(USERNAME);
        usuario.setTipoPermiso(2);
        when(crud.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(crud.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(configCrud.findRotacionAutomaticaCajas(anyString())).thenReturn(Optional.empty());
    }

    private void conCajas(int n) {
        CajaUsuario[] filas = new CajaUsuario[n];
        for (int i = 0; i < n; i++) filas[i] = new CajaUsuario(i + 1, USERNAME, i == 0);
        when(cajaUsuarioCrud.findByIdUsuario(USERNAME)).thenReturn(List.of(filas));
    }

    // ---------- setRotacion(true) ----------

    @Test
    void encender_cajeroConDosCajas_ok() {
        conCajas(2);
        when(configCrud.updateRotacionAutomaticaCajas(USERNAME, 1)).thenReturn(1);

        service.setRotacion(USER_ID, true);

        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 1);
    }

    @Test
    void encender_noCajero_es422() {
        usuario.setTipoPermiso(4);

        assertThatThrownBy(() -> service.setRotacion(USER_ID, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cajero");
        verify(configCrud, never()).updateRotacionAutomaticaCajas(anyString(), anyInt());
    }

    @Test
    void encender_conUnaCaja_es422() {
        conCajas(1);

        assertThatThrownBy(() -> service.setRotacion(USER_ID, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exactamente 2 cajas");
    }

    @Test
    void encender_usuarioLegacySinFilaConfig_es422() {
        conCajas(2);
        when(configCrud.updateRotacionAutomaticaCajas(USERNAME, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.setRotacion(USER_ID, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("config_user_facturacion");
    }

    // ---------- setRotacion(false) ----------

    @Test
    void apagar_siempreOk_sinValidaciones() {
        usuario.setTipoPermiso(4); // ni siquiera cajero
        when(configCrud.updateRotacionAutomaticaCajas(USERNAME, 0)).thenReturn(0); // sin fila

        service.setRotacion(USER_ID, false); // no lanza

        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 0);
        verify(cajaUsuarioCrud, never()).findByIdUsuario(anyString());
    }

    // ---------- auto-apagado en update() ----------

    @Test
    void update_aTipoNoCajero_apagaRotacion() {
        service.update(USER_ID, new UserUpdateRequest(null, 4, null, null));

        verify(configCrud).updateRotacionAutomaticaCajas(USERNAME, 0);
    }

    @Test
    void update_sigueSiendoCajero_noTocaRotacion() {
        service.update(USER_ID, new UserUpdateRequest(null, 2, null, null));

        verify(configCrud, never()).updateRotacionAutomaticaCajas(anyString(), anyInt());
    }

    // ---------- flag en las respuestas ----------

    @Test
    void getById_mapeaElFlag() {
        when(configCrud.findRotacionAutomaticaCajas(USERNAME)).thenReturn(Optional.of(1));

        assertThat(service.getById(USER_ID).rotacionAutomaticaCajas()).isTrue();
    }

    @Test
    void getAll_usaBulkYDefaultFalse() {
        when(crud.findAll()).thenReturn(List.of(usuario));
        when(configCrud.findAllRotacionFlags()).thenReturn(List.<Object[]>of(
                new Object[]{USERNAME, 1}));

        List<UserResponse> all = service.getAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).rotacionAutomaticaCajas()).isTrue();
        // el flag de la lista sale del bulk, no de la query individual
        verify(configCrud, never()).findRotacionAutomaticaCajas(anyString());
    }
}

package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * US-102 — rotación automática de cajas. Réplica de la semántica del Swing
 * ({@code RotacionCajas} + {@code Usuario.nextCaja()}): bandera 0→1→2→0 que
 * rota solo en 0, round-robin por codigo_caja partiendo de la default, y
 * cadencia que solo consumen las ventas a consumidor final.
 */
@ExtendWith(MockitoExtension.class)
class RotacionCajasServiceTest {

    private static final String USER = "tecnico";

    @Mock private ConfigUserFacturacionCRUD configCrud;
    @Mock private CajaUsuarioCRUD cajaUsuarioCrud;
    @Mock private CajaCRUD cajaCrud;

    private RotacionCajasService service;

    @BeforeEach
    void setUp() {
        service = new RotacionCajasService(configCrud, cajaUsuarioCrud, cajaCrud);
    }

    // ---------- helpers ----------

    private static CajaUsuario asignacion(int codigoCaja, boolean porDefecto) {
        return new CajaUsuario(codigoCaja, USER, porDefecto);
    }

    private static Caja caja(int codigo, String nombreDb) {
        Caja c = new Caja();
        c.setCodigo(codigo);
        c.setNombreDb(nombreDb);
        return c;
    }

    /** Config con flag prendido + N cajas para USER (caja 1 default). */
    private void conRotacionYDosCajas() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.of(1));
        when(cajaUsuarioCrud.findByIdUsuario(USER)).thenReturn(List.of(
                asignacion(1, true), asignacion(2, false)));
        lenient().when(cajaCrud.findById(1)).thenReturn(Optional.of(caja(1, "admin_tools_caja_1")));
        lenient().when(cajaCrud.findById(2)).thenReturn(Optional.of(caja(2, "admin_tools_caja_2")));
    }

    // ---------- casos ----------

    @Test
    void flagApagado_nuncaRota_niConsumeBandera() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.of(0));

        for (int i = 0; i < 5; i++) {
            assertThat(service.decideCaja(USER, true)).isEmpty();
        }
        verify(cajaUsuarioCrud, never()).findByIdUsuario(anyString());
    }

    @Test
    void sinFilaDeConfig_esApagado() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.empty());

        assertThat(service.decideCaja(USER, true)).isEmpty();
    }

    @Test
    void clienteIdentificado_noRota_niConsumeBandera() {
        conRotacionYDosCajas();

        // 3 ventas con cliente identificado en el medio no mueven la bandera:
        assertThat(service.decideCaja(USER, false)).isEmpty();
        assertThat(service.decideCaja(USER, false)).isEmpty();
        assertThat(service.decideCaja(USER, false)).isEmpty();

        // la primera CF sigue siendo "bandera 0" → rota
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
    }

    @Test
    void unaSolaCaja_esNoOp() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.of(1));
        when(cajaUsuarioCrud.findByIdUsuario(USER)).thenReturn(List.of(asignacion(1, true)));

        assertThat(service.decideCaja(USER, true)).isEmpty();
        verify(cajaCrud, never()).findById(anyInt());
    }

    @Test
    void cadencia_unaDeCadaTresVentasCF_rota() {
        conRotacionYDosCajas();

        // bandera 0 → rota a la siguiente de la default (caja 2)
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
        // bandera 1 y 2 → default
        assertThat(service.decideCaja(USER, true)).isEmpty();
        assertThat(service.decideCaja(USER, true)).isEmpty();
        // bandera vuelve a 0 → rota de nuevo; con 2 cajas el round-robin
        // hace wrap y devuelve la propia default (setTenant inocuo)
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_1");
        assertThat(service.decideCaja(USER, true)).isEmpty();
        assertThat(service.decideCaja(USER, true)).isEmpty();
        // tercer ciclo: vuelve a caja 2
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
    }

    @Test
    void tresCajas_elRoundRobinAvanzaSoloEnRotadas() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.of(1));
        when(cajaUsuarioCrud.findByIdUsuario(USER)).thenReturn(List.of(
                asignacion(1, true), asignacion(2, false), asignacion(3, false)));
        lenient().when(cajaCrud.findById(2)).thenReturn(Optional.of(caja(2, "admin_tools_caja_2")));
        lenient().when(cajaCrud.findById(3)).thenReturn(Optional.of(caja(3, "admin_tools_caja_3")));

        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2"); // rota
        assertThat(service.decideCaja(USER, true)).isEmpty();
        assertThat(service.decideCaja(USER, true)).isEmpty();
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_3"); // rota
    }

    @Test
    void banderasIndependientesPorUsuario() {
        conRotacionYDosCajas();
        when(configCrud.findRotacionAutomaticaCajas("ana")).thenReturn(Optional.of(1));
        when(cajaUsuarioCrud.findByIdUsuario("ana")).thenReturn(List.of(
                new CajaUsuario(1, "ana", true), new CajaUsuario(2, "ana", false)));

        // tecnico consume su bandera 0
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
        // ana arranca con SU bandera en 0 → también rota
        assertThat(service.decideCaja("ana", true)).contains("admin_tools_caja_2");
        // y tecnico sigue en bandera 1 (no rota)
        assertThat(service.decideCaja(USER, true)).isEmpty();
    }

    @Test
    void cajaDestinoSinNombreDb_quedaEnDefault() {
        when(configCrud.findRotacionAutomaticaCajas(USER)).thenReturn(Optional.of(1));
        when(cajaUsuarioCrud.findByIdUsuario(USER)).thenReturn(List.of(
                asignacion(1, true), asignacion(2, false)));
        when(cajaCrud.findById(2)).thenReturn(Optional.of(caja(2, " ")));

        assertThat(service.decideCaja(USER, true)).isEmpty();
    }

    @Test
    void reset_reiniciaLaCadencia() {
        conRotacionYDosCajas();

        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
        assertThat(service.decideCaja(USER, true)).isEmpty(); // bandera 1
        service.reset(USER);
        // tras reset la bandera vuelve a 0 y el puntero re-parte de la default
        assertThat(service.decideCaja(USER, true)).contains("admin_tools_caja_2");
    }
}

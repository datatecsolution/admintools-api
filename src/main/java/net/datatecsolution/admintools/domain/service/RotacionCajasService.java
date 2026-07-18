package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import net.datatecsolution.admintools.persistence.crud.ConfigUserFacturacionCRUD;
import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * US-102 — Rotación automática de cajas en ventas a CONSUMIDOR FINAL.
 *
 * Paridad con {@code RotacionCajas} + {@code Usuario.nextCaja()} del Swing:
 * con el flag {@code config_user_facturacion.rotacion_automatica_cajas}
 * encendido, 1 de cada 3 ventas a consumidor final del mostrador se factura
 * en la SIGUIENTE caja asignada del usuario (round-robin sobre
 * {@code cajas_usuarios}, orden estable por codigo_caja); las otras 2 y todas
 * las ventas con cliente identificado quedan en la caja por defecto. El
 * propósito es repartir facturas entre los rangos CAI de las cajas del
 * cajero.
 *
 * La cadencia la lleva la "bandera" (0→1→2→0, rota solo cuando vale 0) y el
 * puntero del round-robin, EN MEMORIA por usuario — misma semántica que el
 * Swing, que la mantiene por sesión de la app: reiniciar la API equivale a
 * reiniciar el Swing (los contadores vuelven a 0 y el reparto sigue ~1/3).
 * Cada cliente corre una sola instancia de la API, así que no hay problema
 * de coherencia entre nodos.
 */
@Service
public class RotacionCajasService {

    private static final Logger log = LoggerFactory.getLogger(RotacionCajasService.class);

    /** Estado de rotación de un usuario. Mutar solo bajo synchronized(estado). */
    private static final class Estado {
        int bandera = 0;   // 0..2; la venta CF se rota solo cuando vale 0
        int puntero = -1;  // índice round-robin en la lista ordenada; -1 = aún en la default
    }

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    private final ConfigUserFacturacionCRUD configCrud;
    private final CajaUsuarioCRUD cajaUsuarioCrud;
    private final CajaCRUD cajaCrud;

    public RotacionCajasService(ConfigUserFacturacionCRUD configCrud,
                                CajaUsuarioCRUD cajaUsuarioCrud,
                                CajaCRUD cajaCrud) {
        this.configCrud = configCrud;
        this.cajaUsuarioCrud = cajaUsuarioCrud;
        this.cajaCrud = cajaCrud;
    }

    /**
     * Decide la caja destino de una venta directa de mostrador.
     *
     * @param user            username del cajero (JWT principal)
     * @param consumidorFinal true si el cliente resuelto es CONSUMIDOR FINAL (id 1)
     * @return nombre_db de la caja destino si ESTA venta se rota; empty = queda
     *         en la caja default del tenant. El wrap del round-robin puede
     *         devolver la propia default — setTenant a la misma BD es inocuo.
     */
    public Optional<String> decideCaja(String user, boolean consumidorFinal) {
        // Ventas con cliente identificado nunca rotan NI consumen bandera
        // (el "1 de cada 3" del Swing cuenta solo ventas a consumidor final).
        if (!consumidorFinal || user == null) {
            return Optional.empty();
        }

        // Flag apagado o sin fila → apagado (opt-in explícito, como el Swing).
        // Fuera del lock: es una lectura de BD y el flag no forma parte del estado.
        if (configCrud.findRotacionAutomaticaCajas(user).orElse(0) != 1) {
            return Optional.empty();
        }

        // Cajas asignadas en orden estable (mismo criterio que UserCajaService).
        List<CajaUsuario> cajas = cajaUsuarioCrud.findByIdUsuario(user).stream()
                .sorted(Comparator.comparing(CajaUsuario::getCodigoCaja))
                .collect(Collectors.toList());
        if (cajas.size() < 2) {
            return Optional.empty(); // nada que rotar
        }

        Integer codigoDestino;
        Estado estado = estados.computeIfAbsent(user, k -> new Estado());
        synchronized (estado) {
            int b = estado.bandera;
            estado.bandera = (b + 1) % 3;
            if (b != 0) {
                return Optional.empty(); // 2 de cada 3 CF quedan en la default
            }
            if (estado.puntero < 0) {
                // Arranque: partir de la caja por defecto (o la primera).
                int idxDefault = 0;
                for (int i = 0; i < cajas.size(); i++) {
                    if (cajas.get(i).isPorDefecto()) {
                        idxDefault = i;
                        break;
                    }
                }
                estado.puntero = idxDefault;
            }
            estado.puntero = (estado.puntero + 1) % cajas.size();
            codigoDestino = cajas.get(estado.puntero).getCodigoCaja();
        }

        Optional<String> db = cajaCrud.findById(codigoDestino)
                .map(c -> c.getNombreDb())
                .filter(n -> n != null && !n.isBlank());
        if (db.isEmpty()) {
            log.warn("ROT-1: caja destino {} de {} sin nombre_db — la venta queda en la default",
                    codigoDestino, user);
        }
        return db;
    }

    /** Resetea el estado de un usuario (tests / operativo). */
    void reset(String user) {
        estados.remove(user);
    }
}

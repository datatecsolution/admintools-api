package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.CajaUsuarioCRUD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * US-130 — resolución de la caja efectiva del vendedor, extraída de
 * OrderService para que la búsqueda de productos use LA MISMA bodega que el
 * guard de guardado.
 *
 * Antes la búsqueda leía articulo_view (bodega 1 cableada) y el guard
 * validaba en la bodega de la caja del vendedor: el vendedor veía stock que
 * el guardado le rechazaba ("hay 0 disponibles" tras ver 452 — caso AGUA
 * OXIGENADA, Sharon 2026-08-03). Un solo resolver garantiza que ambos
 * caminos miren el mismo lugar.
 */
@Service
public class CajaVendedorService {

    /** Caja efectiva del vendedor — (codigo, codigo_bodega). */
    public record CajaVendedor(int codigo, int bodega) {}

    @Autowired
    private CajaUsuarioCRUD cajaUsuarioCRUD;

    /**
     * US-109: misma resolución que el TenantInterceptor (cajas_usuarios
     * por_defecto → fallback legacy usuario.codigo_caja). Empty si el usuario
     * no tiene caja por ninguna vía — el caller decide el error (nunca caer
     * en silencio a la caja 1, que era el bug del DEFAULT).
     */
    public Optional<CajaVendedor> resolver(String user) {
        List<Object[]> filas = cajaUsuarioCRUD.findCajaEfectiva(user);
        if (filas.isEmpty()) {
            filas = cajaUsuarioCRUD.findCajaLegacy(user);
        }
        return filas.stream().findFirst()
                .map(fila -> new CajaVendedor(
                        ((Number) fila[0]).intValue(),
                        fila[1] == null ? 1 : ((Number) fila[1]).intValue()));
    }

    /**
     * Bodega para la BÚSQUEDA de productos. A diferencia del guardado (que
     * responde 409 si el vendedor no tiene caja), navegar el catálogo no debe
     * romperse: sin caja se cae a la bodega 1 — el comportamiento histórico
     * de articulo_view — y el 409 del save sigue siendo quien frena.
     */
    public int bodegaParaBusqueda(String user) {
        return resolver(user).map(CajaVendedor::bodega).orElse(1);
    }
}

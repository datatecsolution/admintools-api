package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.CajaUsuario;
import net.datatecsolution.admintools.persistence.entity.CajaUsuarioKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Sprint 4.5 fix — asignaciones caja ↔ usuario.
 */
public interface CajaUsuarioCRUD extends JpaRepository<CajaUsuario, CajaUsuarioKey> {

    /** Todas las cajas asignadas a un username. */
    List<CajaUsuario> findByIdUsuario(String usuario);

    /** Borra todas las asignaciones de un usuario (al hacer replace). */
    void deleteByIdUsuario(String usuario);

    /**
     * US-109: caja efectiva de un usuario — MISMA resolución que el
     * TenantInterceptor (la por_defecto de cajas_usuarios, o la de menor
     * código) pero devolviendo (codigo, codigo_bodega) en vez de nombre_db.
     * El pedido hereda esta caja para que la reserva de stock caiga en la
     * bodega real del vendedor. Fila = [codigo, codigo_bodega].
     * (List y no Optional&lt;Object[]&gt;: Spring Data anida la fila multi-columna
     * dentro del Optional — ClassCastException en runtime.)
     */
    @Query(value = """
            SELECT c.codigo, c.codigo_bodega
              FROM cajas_usuarios cu
              JOIN cajas c ON c.codigo = cu.codigo_caja
             WHERE cu.usuario = :usuario
             ORDER BY cu.por_defecto DESC, cu.codigo_caja ASC
             LIMIT 1
            """, nativeQuery = true)
    List<Object[]> findCajaEfectiva(@Param("usuario") String usuario);

    /** US-109: fallback legacy usuario.codigo_caja (> 0), como el interceptor. */
    @Query(value = """
            SELECT c.codigo, c.codigo_bodega
              FROM usuario u
              JOIN cajas c ON c.codigo = u.codigo_caja
             WHERE u.usuario = :usuario AND u.codigo_caja > 0
             LIMIT 1
            """, nativeQuery = true)
    List<Object[]> findCajaLegacy(@Param("usuario") String usuario);
}

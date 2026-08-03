package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteCRUD extends JpaRepository<Cliente, Integer> {
    List<Cliente> findByNombre(String nombre);
    List<Cliente> findByNombreContainingOrderByNombreAsc(String nombre);

    // US-019: variante paginada de la busqueda por vendedor (GET /customers).
    // Solo clientes GESTIONADOS (tipo_cliente=2): los de contado/escritos al vuelo
    // (tipo 1, p.ej. Consumidor final) no se listan en admin ni en la búsqueda del POS.
    @Query(value = "SELECT p.* FROM cliente AS p " +
            " INNER JOIN empleados ON p.id_vendedor=empleados.codigo_empleado " +
            " INNER JOIN usuario ON empleados.codigo_empleado=usuario.codigo_empleado " +
            " WHERE p.tipo_cliente=2 AND p.nombre_cliente LIKE %:descripcion% AND usuario.usuario = :userId ORDER BY p.nombre_cliente ASC",
            countQuery = "SELECT count(*) FROM cliente AS p " +
            " INNER JOIN empleados ON p.id_vendedor=empleados.codigo_empleado " +
            " INNER JOIN usuario ON empleados.codigo_empleado=usuario.codigo_empleado " +
            " WHERE p.tipo_cliente=2 AND p.nombre_cliente LIKE %:descripcion% AND usuario.usuario = :userId",
            nativeQuery = true)
    Page<Cliente> searchByVendedor(@Param("descripcion") String descripcion, @Param("userId") String userId, Pageable pageable);
}

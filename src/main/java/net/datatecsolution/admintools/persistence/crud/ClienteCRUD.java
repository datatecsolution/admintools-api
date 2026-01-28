package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteCRUD extends JpaRepository<Cliente, Integer> {
    List<Cliente> findByNombre(String nombre);
    List<Cliente> findByNombreContainingOrderByNombreAsc(String nombre);

    @Query(value = "SELECT p.codigo_cliente, " +
            " p.nombre_cliente, " +
            " p.direccion, " +
            " p.telefono, " +
            " p.movil, " +
            " p.rtn, " +
            " p.zona_procedencia, " +
            " p.codigo_postal, " +
            " p.codiciones_pago, " +
            " p.limite_credito, " +
            " p.saldo, " +
            " p.clasificacion, " +
            " p.estado, " +
            " p.tipo_cliente, " +
            " p.id_vendedor, " +
            " p.id_ruta_cobro " +
            " FROM cliente AS p " +
                " INNER JOIN empleados ON p.id_vendedor=empleados.codigo_empleado " +
                " INNER JOIN usuario ON empleados.codigo_empleado=usuario.codigo_empleado " +
            " WHERE p.nombre_cliente LIKE  %:descripcion% AND usuario.usuario = :userId ORDER BY p.nombre_cliente ASC", nativeQuery = true)
    List<Cliente> findByNombreVendedorOrderByNombreAsc(@Param("descripcion") String descripcion,  @Param("userId") String userId);
}

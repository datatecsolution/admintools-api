package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.DetalleRequisicion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleRequisicionCRUD extends JpaRepository<DetalleRequisicion, Integer> {

    List<DetalleRequisicion> findByCodigoRequisicion(Integer codigoRequisicion);
}

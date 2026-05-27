package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DepartamentoCRUD extends JpaRepository<Departamento, Integer> {

    @Query("SELECT COALESCE(MAX(d.codigoDepartamento), 0) FROM Departamento d")
    int findMaxCodigoDepartamento();
}

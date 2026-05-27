package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BodegaCRUD extends JpaRepository<Bodega, Integer> {

    @Query("SELECT COALESCE(MAX(b.codigoBodega), 0) FROM Bodega b")
    int findMaxCodigoBodega();
}

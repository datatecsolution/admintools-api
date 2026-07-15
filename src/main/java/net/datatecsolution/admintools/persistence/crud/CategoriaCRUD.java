package net.datatecsolution.admintools.persistence.crud;

import net.datatecsolution.admintools.persistence.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaCRUD extends JpaRepository<Categoria, Integer> {

    /**
     * Cuenta articulos asignados a la categoria. Usado para impedir
     * el DELETE si hay dependencias. Cero implica que es seguro borrar.
     */
    @Query("SELECT COUNT(a) FROM Articulo a WHERE a.idCategoria = :id")
    long countArticulos(@Param("id") Integer id);

    /** US-081: hijos directos de una categoría (para bloquear DELETE con 409). */
    long countByParentId(Integer parentId);
}

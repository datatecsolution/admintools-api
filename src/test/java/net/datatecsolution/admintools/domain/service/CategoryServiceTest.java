package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.dto.CategoryRequest;
import net.datatecsolution.admintools.domain.dto.CategoryTreeNode;
import net.datatecsolution.admintools.persistence.crud.CategoriaCRUD;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import net.datatecsolution.admintools.persistence.mapper.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * US-081 — árbol de categorías y validación de ciclos. Foco en getTree
 * (anidado, huérfanos como raíz) y validarParent ejercitado vía update
 * (ciclo, self-parent, parent inexistente).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoriaCRUD crud;
    @Mock private CategoryMapper mapper;

    private CategoryService service() {
        return new CategoryService(crud, mapper);
    }

    private Categoria cat(int id, String nombre, Integer parentId) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setDescripcion(nombre);
        c.setParentId(parentId);
        c.setMostrarPos(Boolean.TRUE);
        return c;
    }

    private CategoryRequest req(Integer parentId) {
        return new CategoryRequest("Nueva", "desc", true, parentId);
    }

    @Test
    void getTree_anidaPadreHijoNieto() {
        Categoria padre = cat(1, "Bebidas", null);
        Categoria hijo = cat(2, "Gaseosas", 1);
        Categoria nieto = cat(3, "Colas", 2);
        when(crud.findAll()).thenReturn(List.of(padre, hijo, nieto));

        List<CategoryTreeNode> tree = service().getTree();

        assertThat(tree).hasSize(1);
        CategoryTreeNode raiz = tree.get(0);
        assertThat(raiz.id()).isEqualTo(1);
        assertThat(raiz.children()).hasSize(1);
        assertThat(raiz.children().get(0).id()).isEqualTo(2);
        assertThat(raiz.children().get(0).children().get(0).id()).isEqualTo(3);
    }

    @Test
    void getTree_huerfanoConParentInexistente_seTrataComoRaiz() {
        Categoria raizReal = cat(1, "Bebidas", null);
        Categoria huerfano = cat(5, "Suelto", 999); // parent 999 no existe
        when(crud.findAll()).thenReturn(List.of(raizReal, huerfano));

        List<CategoryTreeNode> tree = service().getTree();

        assertThat(tree).extracting(CategoryTreeNode::id)
                .containsExactlyInAnyOrder(1, 5);
    }

    @Test
    void update_parentEsDescendiente_lanza409PorCiclo() {
        // 1 -> 2 -> 3 ; intentar poner parent de 1 = 3 forma un ciclo
        Categoria uno = cat(1, "Uno", null);
        Categoria dos = cat(2, "Dos", 1);
        Categoria tres = cat(3, "Tres", 2);
        when(crud.findById(1)).thenReturn(Optional.of(uno));
        when(crud.findById(3)).thenReturn(Optional.of(tres));
        when(crud.findById(2)).thenReturn(Optional.of(dos));

        assertThatThrownBy(() -> service().update(1, req(3)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_selfParent_lanza409() {
        when(crud.findById(1)).thenReturn(Optional.of(cat(1, "Uno", null)));

        assertThatThrownBy(() -> service().update(1, req(1)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_parentInexistente_lanzaIllegalArgument() {
        when(crud.findById(1)).thenReturn(Optional.of(cat(1, "Uno", null)));
        when(crud.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(1, req(99)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

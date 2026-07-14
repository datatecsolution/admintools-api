package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.CategoryRequest;
import net.datatecsolution.admintools.domain.dto.CategoryResponse;
import net.datatecsolution.admintools.domain.dto.CategoryTreeNode;
import net.datatecsolution.admintools.persistence.crud.CategoriaCRUD;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import net.datatecsolution.admintools.persistence.mapper.CategoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 4 #50 — CRUD de categorias de producto.
 * US-081 — jerarquia padre-hijo (marcas.parent_id, V38): arbol, validacion
 * de ciclos y DELETE bloqueado si hay hijos.
 *
 * El DELETE valida que no haya articulos asignados (con consulta JPQL
 * directa al CRUD) ni subcategorias. Si hay, 409 Conflict — el frontend
 * muestra el detalle para que el usuario reasigne antes de borrar.
 */
@Service
public class CategoryService {

    private final CategoriaCRUD crud;
    private final CategoryMapper mapper;

    public CategoryService(CategoriaCRUD crud, CategoryMapper mapper) {
        this.crud = crud;
        this.mapper = mapper;
    }

    public List<CategoryResponse> getAll() {
        return crud.findAll().stream().map(mapper::toResponse).toList();
    }

    public CategoryResponse getById(int id) {
        Categoria entity = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria " + id + " no encontrada"));
        return mapper.toResponse(entity);
    }

    /**
     * US-081 — arbol completo de categorias. Una sola query; el armado es
     * en memoria (el catalogo de categorias es chico). Nodos huerfanos
     * (parent borrado a mano en BD) se tratan como raices para no perderlos.
     */
    public List<CategoryTreeNode> getTree() {
        List<Categoria> todas = crud.findAll();
        Map<Integer, List<Categoria>> hijosPorPadre = new HashMap<>();
        Set<Integer> ids = new HashSet<>();
        for (Categoria c : todas) {
            ids.add(c.getId());
        }
        List<Categoria> raices = new ArrayList<>();
        for (Categoria c : todas) {
            if (c.getParentId() == null || !ids.contains(c.getParentId())) {
                raices.add(c);
            } else {
                hijosPorPadre.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        return buildNodes(raices, hijosPorPadre);
    }

    private List<CategoryTreeNode> buildNodes(List<Categoria> nivel,
                                              Map<Integer, List<Categoria>> hijosPorPadre) {
        return nivel.stream()
                .sorted(Comparator.comparing(Categoria::getDescripcion,
                        String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CategoryTreeNode(
                        c.getId(),
                        c.getDescripcion(),
                        c.getObservacion(),
                        c.getMostrarPos(),
                        c.getParentId(),
                        buildNodes(hijosPorPadre.getOrDefault(c.getId(), List.of()),
                                hijosPorPadre)))
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        validarParent(null, request.parentId());
        Categoria entity = mapper.fromRequest(request);
        entity.setMostrarPos(Boolean.TRUE.equals(request.posVisible()));
        entity.setParentId(request.parentId());
        return mapper.toResponse(crud.save(entity));
    }

    public CategoryResponse update(int id, CategoryRequest request) {
        Categoria existing = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria " + id + " no encontrada"));
        validarParent(id, request.parentId());
        existing.setDescripcion(request.name());
        existing.setObservacion(request.description());
        existing.setMostrarPos(Boolean.TRUE.equals(request.posVisible()));
        existing.setParentId(request.parentId());
        return mapper.toResponse(crud.save(existing));
    }

    public void delete(int id) {
        Categoria existing = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria " + id + " no encontrada"));
        long productos = crud.countArticulos(id);
        if (productos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la categoria tiene " + productos +
                    " producto(s) asignado(s). Reasigna o elimina los productos antes.");
        }
        long hijos = crud.countByParentId(id);
        if (hijos > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la categoria tiene " + hijos +
                    " subcategoria(s). Reasigna o elimina las subcategorias antes.");
        }
        crud.delete(existing);
    }

    /**
     * US-081 — valida el parent propuesto: debe existir, no ser la propia
     * categoria y no ser un descendiente (eso formaria un ciclo). El walk
     * sube por parent_id con guarda de visitados por si la BD ya tuviera un
     * ciclo creado a mano.
     */
    private void validarParent(Integer id, Integer parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una categoria no puede ser su propio padre");
        }
        Categoria parent = crud.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoria padre " + parentId + " no existe"));
        if (id == null) {
            return; // create: un parent existente nunca forma ciclo
        }
        Set<Integer> visitados = new HashSet<>();
        Categoria cursor = parent;
        while (cursor != null && cursor.getParentId() != null) {
            Integer arriba = cursor.getParentId();
            if (arriba.equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Jerarquia invalida: la categoria padre propuesta es descendiente de esta categoria (ciclo)");
            }
            if (!visitados.add(arriba)) {
                break; // ciclo preexistente en datos: cortar el walk
            }
            cursor = crud.findById(arriba).orElse(null);
        }
    }
}

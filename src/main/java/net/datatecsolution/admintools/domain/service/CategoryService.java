package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.CategoryRequest;
import net.datatecsolution.admintools.domain.dto.CategoryResponse;
import net.datatecsolution.admintools.persistence.crud.CategoriaCRUD;
import net.datatecsolution.admintools.persistence.entity.Categoria;
import net.datatecsolution.admintools.persistence.mapper.CategoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Sprint 4 #50 — CRUD de categorias de producto.
 *
 * El DELETE valida que no haya articulos asignados (con consulta JPQL
 * directa al CRUD). Si hay, 409 Conflict — el frontend muestra el conteo
 * al cajero para que reasigne antes de borrar.
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

    public CategoryResponse create(CategoryRequest request) {
        Categoria entity = mapper.fromRequest(request);
        entity.setMostrarPos(Boolean.TRUE.equals(request.posVisible()));
        return mapper.toResponse(crud.save(entity));
    }

    public CategoryResponse update(int id, CategoryRequest request) {
        Categoria existing = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoria " + id + " no encontrada"));
        existing.setDescripcion(request.name());
        existing.setObservacion(request.description());
        existing.setMostrarPos(Boolean.TRUE.equals(request.posVisible()));
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
        crud.delete(existing);
    }
}

package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * US-081 — nodo del árbol de categorías (GET /categories/tree).
 * children viene ordenado por nombre; una categoría raíz tiene parentId null.
 */
public record CategoryTreeNode(
        Integer id,
        String name,
        String description,
        Boolean posVisible,
        Integer parentId,
        List<CategoryTreeNode> children
) {
}

package net.datatecsolution.admintools.domain.dto;

/**
 * US-043/044 — error de validación de una fila del archivo de import.
 * row es 1-based contando el header como fila 1 (la primera fila de datos
 * es la 2, igual que la ve el usuario en Excel).
 */
public record ImportError(
        int row,
        String column,
        String message
) {
}

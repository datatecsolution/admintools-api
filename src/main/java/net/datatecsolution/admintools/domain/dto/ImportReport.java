package net.datatecsolution.admintools.domain.dto;

import java.util.List;

/**
 * US-043/044 — resultado de un import masivo (dry-run o real).
 * Con dryRun=true nada se persiste: validRows dice cuántas filas pasarían.
 * Con dryRun=false el import es todo-o-nada: si errors no está vacío no se
 * importó NADA (el controller responde 400 con este mismo body).
 */
public record ImportReport(
        int totalRows,
        int validRows,
        int importedRows,
        boolean dryRun,
        List<ImportError> errors
) {
}

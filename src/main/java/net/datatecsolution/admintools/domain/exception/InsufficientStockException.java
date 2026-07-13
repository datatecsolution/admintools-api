package net.datatecsolution.admintools.domain.exception;

import net.datatecsolution.admintools.domain.dto.StockConflict;

import java.util.List;

/**
 * US-074: la orden pide más stock del disponible y el usuario está
 * bloqueado para sobrevender (config_user_facturacion.facturar_sin_inventario = 0).
 * El GlobalExceptionHandler la traduce a 409 con el detalle por producto.
 */
public class InsufficientStockException extends RuntimeException {

    private final List<StockConflict> conflicts;

    public InsufficientStockException(List<StockConflict> conflicts) {
        super("Stock insuficiente para uno o más productos de la orden");
        this.conflicts = conflicts;
    }

    public List<StockConflict> getConflicts() {
        return conflicts;
    }
}

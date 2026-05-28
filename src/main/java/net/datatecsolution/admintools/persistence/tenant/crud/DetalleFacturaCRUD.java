package net.datatecsolution.admintools.persistence.tenant.crud;

import net.datatecsolution.admintools.persistence.tenant.entity.DetalleFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DetalleFacturaCRUD extends JpaRepository<DetalleFactura, Integer> {

    List<DetalleFactura> findByNumeroFactura(Integer numeroFactura);

    /**
     * Suma de cantidad facturada de un articulo en una factura especifica.
     * Devuelve 0 si no hay filas. Usada por Sale Returns para validar que
     * no se devuelva mas de lo facturado.
     *
     * Corre sobre tenantEMF — la caja se resuelve via TenantContext.
     */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleFactura d " +
           "WHERE d.numeroFactura = :nf AND d.codigoArticulo = :art")
    BigDecimal sumCantidadByFacturaArticulo(@Param("nf") Integer nf,
                                            @Param("art") Integer art);
}

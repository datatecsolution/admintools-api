package net.datatecsolution.admintools.persistence.mapper;

import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.dto.ProductResponse;
import net.datatecsolution.admintools.domain.dto.ProductStock;
import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-146 — la bandera {@code sePesa} viaja completa por el mapeo y respeta
 * el contrato "null = no tocar" en PUT (mismo patrón que barcodes).
 *
 * Se prueba contra la implementación generada por MapStruct: es donde vive
 * el comportamiento real de NullValuePropertyMappingStrategy.IGNORE.
 */
class ProductMasterMapperSePesaTest {

    private final ProductMasterMapper mapper = new ProductMasterMapperImpl();

    private ProductRequest req(Boolean sePesa) {
        return new ProductRequest("FRIJOL ROJO", new BigDecimal("28.00"),
                1, 1, 0, 1, true, List.of(), sePesa);
    }

    @Test
    void toEntity_conTrue_llegaTrue() {
        ArticuloMaster e = mapper.toEntity(req(true));
        assertThat(e.getSePesa()).isTrue();
    }

    /** POST sin la bandera: toEntity escribe null (pisa el inicializador del
     *  entity) — el default a false lo pone el service en create(). Este test
     *  documenta POR QUE esa línea del service es necesaria. */
    @Test
    void toEntity_conNull_quedaNull_elServiceDebeDefaultear() {
        ArticuloMaster e = mapper.toEntity(req(null));
        assertThat(e.getSePesa()).isNull();
    }

    /** PUT con null NO debe apagar la bandera de un producto pesado. */
    @Test
    void updateEntity_conNull_preservaElValorExistente() {
        ArticuloMaster e = new ArticuloMaster();
        e.setSePesa(Boolean.TRUE);

        mapper.updateEntity(req(null), e);

        assertThat(e.getSePesa()).isTrue();
    }

    @Test
    void updateEntity_conFalse_siApaga() {
        ArticuloMaster e = new ArticuloMaster();
        e.setSePesa(Boolean.TRUE);

        mapper.updateEntity(req(false), e);

        assertThat(e.getSePesa()).isFalse();
    }

    @Test
    void toResponse_incluyeLaBandera() {
        ArticuloMaster e = new ArticuloMaster();
        e.setCodigoArticulo(7);
        e.setArticulo("FRIJOL ROJO");
        e.setPrecioArticulo(28.0);
        e.setSePesa(Boolean.TRUE);

        ProductResponse r = mapper.toResponse(e);

        assertThat(r.sePesa()).isTrue();
    }

    /** conStock copia los campos a mano: si se olvida sePesa, el listado con
     *  bodega (la vista que consume el POS) perdería la bandera. */
    @Test
    void conStock_noPierdeLaBandera() {
        ProductResponse r = new ProductResponse(7, "FRIJOL ROJO", new BigDecimal("28.00"),
                1, 1, 0, 1, true, List.of(), null, true, null);

        ProductResponse conStock = r.conStock(new ProductStock(
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, "ok"));

        assertThat(conStock.sePesa()).isTrue();
        assertThat(conStock.stock()).isNotNull();
    }
}

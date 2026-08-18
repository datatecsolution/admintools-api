package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.dto.ProductRequest;
import net.datatecsolution.admintools.domain.dto.ProductResponse;
import net.datatecsolution.admintools.domain.dto.ProductStock;
import net.datatecsolution.admintools.persistence.crud.ArticuloMasterCRUD;
import net.datatecsolution.admintools.persistence.crud.CajaCRUD;
import net.datatecsolution.admintools.persistence.crud.PreciosArticuloCRUD;
import net.datatecsolution.admintools.persistence.crud.ArticuloKardexCRUD;
import net.datatecsolution.admintools.persistence.crud.ProductStockView;
import net.datatecsolution.admintools.persistence.entity.ArticuloMaster;
import net.datatecsolution.admintools.persistence.entity.Caja;
import net.datatecsolution.admintools.persistence.entity.PrecioArticulo;
import net.datatecsolution.admintools.persistence.mapper.ProductMasterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CRUD del master de productos (INV-4). Trabaja contra la tabla
 * {@code articulo} via {@link ArticuloMaster} (entidad escribible),
 * NO contra {@code articulo_view} (que es @Immutable).
 *
 * El stock NO se gestiona aqui: vive en
 * {@code existencia_articulo_bodega} y se consulta por {@code StockService}.
 *
 * Códigos de barra (US Productos): N por artículo en {@code codigos_articulos}
 * (String, soportan ceros a la izquierda). Se sincronizan con delete+insert y
 * se valida unicidad global (409 si el código pertenece a otro artículo). El
 * {@code altCode} legacy (Integer) se conserva aparte.
 */
@Service
public class ProductMasterService {

    /** Tipo de precio "Publico General" (precios.codigo_precio) — el obligatorio. */
    private static final int PRECIO_PUBLICO_GENERAL = 1;

    /** Para validar el nombre de BD de caja antes de interpolarlo en SQL cross-DB. */
    private static final Pattern SAFE_DB = Pattern.compile("[A-Za-z0-9_]+");

    @Autowired private ArticuloMasterCRUD crud;
    @Autowired private ProductMasterMapper mapper;
    @Autowired private PreciosArticuloCRUD preciosArticuloCRUD;
    @Autowired private ArticuloKardexCRUD articuloKardexCRUD;

    /** JdbcTemplate sobre la BD común (codigos_articulos / compras / cross-DB ventas). */
    private final JdbcTemplate jdbc;
    private final CajaCRUD cajaCRUD;

    public ProductMasterService(@Qualifier("commonDataSource") DataSource commonDS, CajaCRUD cajaCRUD) {
        this.jdbc = new JdbcTemplate(commonDS);
        this.cajaCRUD = cajaCRUD;
    }

    public Page<ProductResponse> search(String name, Integer category, int page, int size) {
        return search(name, category, null, null, null, page, size);
    }

    /**
     * US-140 — los cuatro filtros de la pantalla de Productos, COMBINABLES y
     * resueltos en la base.
     *
     * Antes eran tres ramas excluyentes (texto / categoria / todo) y el texto
     * ganaba: buscar "COCA" filtrando por ABARROTERIA traia tambien las de
     * BEBIDAS. Estado y existencia ni siquiera llegaban aca — el POS los
     * aplicaba sobre la pagina ya recibida, asi que con catalogos grandes
     * mostraban resultados incompletos.
     *
     * El orden por id DESC se mantiene (Sprint 4.5+): los productos recien
     * creados tienen que caer en la primera pagina. Va dentro del query
     * nativo, por eso el Pageable viaja SIN sort.
     *
     * @param active null = todos, true = solo activos, false = solo inactivos
     * @param stock  null = todos, "low" = bajo el minimo, "out" = agotado
     * @param warehouse bodega sobre la que se evalua el stock; obligatoria si
     *                  se filtra por existencia
     */
    public Page<ProductResponse> search(String name, Integer category, Boolean active,
                                        String stock, Integer warehouse, int page, int size) {
        String term = (name != null && !name.isBlank()) ? name.trim() : null;
        String nivel = (stock != null && !stock.isBlank()) ? stock.trim().toLowerCase() : null;

        // ResponseStatusException y no IllegalArgumentException: el
        // GlobalExceptionHandler traduce la primera a 400 y la segunda caeria
        // al generico, devolviendo un 500 que no describe el problema.
        if (nivel != null && !nivel.equals("low") && !nivel.equals("out")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "stock debe ser 'low' (bajo el minimo) o 'out' (agotado)");
        }
        if (nivel != null && warehouse == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para filtrar por existencia hay que indicar la bodega (warehouse)");
        }

        Page<ArticuloMaster> result = crud.searchFiltered(
                term, category,
                active == null ? null : (active ? 1 : 0),
                nivel, warehouse,
                PageRequest.of(page, size));

        // El precio del catálogo es el Precio Público General (precios_articulos,
        // codigo_precio=1), NO la columna legacy articulo.precio_articulo (que es
        // derivada y puede estar desactualizada). Una query por página.
        Map<Integer, BigDecimal> publico = preciosPublicoGeneral(result.getContent());
        // Códigos de barra en bloque (una query por página).
        Map<Integer, List<String>> barcodes = barcodesByProduct(
                result.getContent().stream().map(ArticuloMaster::getCodigoArticulo).toList());
        // Versión de imagen en bloque (US-079; solo el id_img, el blob nunca viaja acá).
        Map<Integer, Integer> images = imagesByProduct(
                result.getContent().stream().map(ArticuloMaster::getCodigoArticulo).toList());
        // US-141: existencia de la bodega, tambien en bloque. Sin bodega no hay
        // stock que adjuntar y el campo viaja en null.
        Map<Integer, ProductStock> stocks = stockByProduct(warehouse,
                result.getContent().stream().map(ArticuloMaster::getCodigoArticulo).toList());
        return result.map(e -> {
            ProductResponse r = conPrecioPublico(mapper.toResponse(e), e, publico, barcodes, images);
            if (warehouse == null) {
                return r;
            }
            // Un articulo sin fila de existencia no es un error: es stock cero.
            return r.conStock(stocks.getOrDefault(e.getCodigoArticulo(), ProductStock.vacio()));
        });
    }

    /**
     * US-141 — existencia de los articulos de ESTA pagina en una bodega.
     * Una sola query con IN, igual que precios/barcodes/imagenes.
     */
    private Map<Integer, ProductStock> stockByProduct(Integer warehouse, List<Integer> ids) {
        Map<Integer, ProductStock> map = new HashMap<>();
        if (warehouse == null || ids.isEmpty()) {
            return map;
        }
        for (ProductStockView v : articuloKardexCRUD.findStockByProducts(warehouse, ids)) {
            map.put(v.getCodigoArticulo(), ProductStock.de(
                    v.getCantidad(), v.getReservado(), v.getDisponible(),
                    v.getCostoUnitario(), v.getCantidadMinima()));
        }
        return map;
    }

    private Map<Integer, BigDecimal> preciosPublicoGeneral(List<ArticuloMaster> items) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        if (items.isEmpty()) {
            return map;
        }
        List<Integer> ids = items.stream().map(ArticuloMaster::getCodigoArticulo).toList();
        for (PrecioArticulo p : preciosArticuloCRUD.findByPrecioIdAndArticuloIdIn(PRECIO_PUBLICO_GENERAL, ids)) {
            map.put(p.getArticuloId(), p.getPrecioArticulo());
        }
        return map;
    }

    /** Carga en bloque los códigos de barra de un conjunto de artículos (orden estable). */
    private Map<Integer, List<String>> barcodesByProduct(List<Integer> ids) {
        Map<Integer, List<String>> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT codigo_articulo, codigo_barra FROM codigos_articulos "
                        + "WHERE codigo_articulo IN (" + in + ") ORDER BY id_codigo",
                rs -> {
                    Integer art = rs.getInt("codigo_articulo");
                    String code = rs.getString("codigo_barra");
                    if (code != null) {
                        map.computeIfAbsent(art, k -> new ArrayList<>()).add(code);
                    }
                }, ids.toArray());
        return map;
    }

    private List<String> barcodesOf(int id) {
        return barcodesByProduct(List.of(id)).getOrDefault(id, List.of());
    }

    /** Carga en bloque el id_img vigente por artículo (US-079). */
    private Map<Integer, Integer> imagesByProduct(List<Integer> ids) {
        Map<Integer, Integer> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT codigo_articulo, MAX(id_img) AS id_img FROM articulo_imagen "
                        + "WHERE codigo_articulo IN (" + in + ") GROUP BY codigo_articulo",
                rs -> {
                    map.put(rs.getInt("codigo_articulo"), rs.getInt("id_img"));
                }, ids.toArray());
        return map;
    }

    private Integer imageVersionOf(int id) {
        return imagesByProduct(List.of(id)).get(id);
    }

    private ProductResponse conPrecioPublico(ProductResponse r, ArticuloMaster e,
                                             Map<Integer, BigDecimal> publico,
                                             Map<Integer, List<String>> barcodes,
                                             Map<Integer, Integer> images) {
        BigDecimal price = publico.get(e.getCodigoArticulo());
        if (price == null) {
            // fallback: legacy precio_articulo si el producto no tiene precio público asignado
            price = e.getPrecioArticulo() == null ? BigDecimal.ZERO : BigDecimal.valueOf(e.getPrecioArticulo());
        }
        // stock = null: lo adjunta search() cuando la peticion trae bodega.
        return new ProductResponse(r.id(), r.name(), price, r.categoryId(),
                r.taxId(), r.altCode(), r.type(), r.active(),
                barcodes.getOrDefault(e.getCodigoArticulo(), List.of()),
                images.get(e.getCodigoArticulo()), null);
    }

    private ProductResponse withBarcodes(ProductResponse r, List<String> barcodes, Integer imageVersion) {
        // Alta/edicion: no hay bodega en el contexto, el stock no aplica.
        return new ProductResponse(r.id(), r.name(), r.price(), r.categoryId(),
                r.taxId(), r.altCode(), r.type(), r.active(), barcodes, imageVersion, null);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        ArticuloMaster entity = mapper.toEntity(request);
        ArticuloMaster saved = crud.save(entity);
        syncBarcodes(saved.getCodigoArticulo(), request.barcodes());
        return withBarcodes(mapper.toResponse(saved), barcodesOf(saved.getCodigoArticulo()),
                imageVersionOf(saved.getCodigoArticulo()));
    }

    @Transactional
    public ProductResponse update(int id, ProductRequest request) {
        ArticuloMaster entity = crud.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product " + id + " not found"));
        mapper.updateEntity(request, entity);
        ArticuloMaster saved = crud.save(entity);
        syncBarcodes(id, request.barcodes());
        return withBarcodes(mapper.toResponse(saved), barcodesOf(id), imageVersionOf(id));
    }

    /**
     * Reemplaza los códigos de barra del artículo (delete + insert), validando
     * unicidad global. {@code null} = no tocar (PUT que no envía barcodes).
     * Lista vacía = limpiar todos. Lanza 409 si algún código ya pertenece a
     * otro artículo.
     */
    private void syncBarcodes(int id, List<String> raw) {
        if (raw == null) {
            return; // edición que no gestiona barcodes: dejar los existentes
        }
        // normalizar: trim, descartar vacíos, sin duplicados (preservando orden)
        List<String> codes = new ArrayList<>(new LinkedHashSet<>(
                raw.stream()
                        .filter(s -> s != null)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()));

        if (!codes.isEmpty()) {
            String in = codes.stream().map(x -> "?").collect(Collectors.joining(","));
            Object[] args = new Object[codes.size() + 1];
            for (int i = 0; i < codes.size(); i++) {
                args[i] = codes.get(i);
            }
            args[codes.size()] = id;
            List<String> dupes = jdbc.queryForList(
                    "SELECT DISTINCT codigo_barra FROM codigos_articulos "
                            + "WHERE codigo_barra IN (" + in + ") AND codigo_articulo <> ?",
                    String.class, args);
            if (!dupes.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Código(s) de barra en uso por otro producto: " + String.join(", ", dupes));
            }
        }

        jdbc.update("DELETE FROM codigos_articulos WHERE codigo_articulo = ?", id);
        if (!codes.isEmpty()) {
            jdbc.batchUpdate(
                    "INSERT INTO codigos_articulos (codigo_articulo, codigo_barra) VALUES (?, ?)",
                    codes.stream().map(c -> new Object[]{id, c}).toList());
        }
    }

    @Transactional
    public void delete(int id) {
        if (!crud.existsById(id)) {
            throw new EntityNotFoundException("Product " + id + " not found");
        }
        if (enUso(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto está referenciado en facturas, compras u órdenes pendientes; "
                            + "no se puede eliminar. Deséchelo dando de baja (inactivar) en su lugar.");
        }
        // producto sin transacciones: limpiar filas hijas propias y borrar el master.
        jdbc.update("DELETE FROM codigos_articulos WHERE codigo_articulo = ?", id);
        jdbc.update("DELETE FROM precios_articulos WHERE codigo_articulo = ?", id);
        jdbc.update("DELETE FROM existencia_articulo_bodega WHERE codigo_articulo = ?", id);
        jdbc.update("DELETE FROM articulo_kardex WHERE codigo_articulo = ?", id);
        try {
            crud.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            // red de seguridad: alguna FK no anticipada → 409 en vez de 500.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto tiene referencias que impiden eliminarlo; inactívelo en su lugar.");
        }
    }

    /**
     * ¿El producto está referenciado por alguna transacción? Compras y
     * devoluciones de compra viven en la BD común; las ventas
     * ({@code detalle_factura}) están particionadas por caja, así que se hace
     * fan-out sobre cada BD de caja.
     */
    private boolean enUso(int id) {
        // US-147: pedidos/cotizaciones VIVOS (Activa=1 / Modificada=2) también
        // referencian el producto. Antes esto no se validaba: se podía borrar
        // un artículo con órdenes pendientes, la relación a articulo_view
        // hidrataba null y la Lista de órdenes completa devolvía 500
        // (Mariposas, orden 79). Las órdenes cerradas (facturada/enviada/
        // anulada) no bloquean: sus líneas huérfanas las tolera el listado.
        Integer enOrdenes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalle_factura_temp d "
                        + "JOIN encabezado_factura_temp e ON e.numero_factura = d.numero_factura "
                        + "WHERE d.codigo_articulo = ? AND e.estado IN (1,2)",
                Integer.class, id);
        if (enOrdenes != null && enOrdenes > 0) {
            return true;
        }
        Integer compras = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalle_factura_compra WHERE codigo_articulo = ?",
                Integer.class, id);
        if (compras != null && compras > 0) {
            return true;
        }
        Integer devCompras = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalle_devoluciones_compra WHERE codigo_articulo = ?",
                Integer.class, id);
        if (devCompras != null && devCompras > 0) {
            return true;
        }
        for (Caja caja : cajaCRUD.findAll()) {
            String db = caja.getNombreDb();
            if (db == null || !SAFE_DB.matcher(db).matches()) {
                continue;
            }
            Integer ventas = jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM " + db + ".detalle_factura WHERE codigo_articulo = ? LIMIT 1)",
                    Integer.class, id);
            if (ventas != null && ventas > 0) {
                return true;
            }
        }
        return false;
    }
}

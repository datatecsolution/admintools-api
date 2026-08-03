package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Customer;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.repository.CustomerRepository;
import net.datatecsolution.admintools.persistence.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service de clientes. Reemplaza la antigua {@code CostomerService}
 * (typo historico). Conserva las dos puertas de entrada:

 *  - search/create/update/delete con DTOs (US-019 /customers)
 */
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private SellerCatalogService sellerCatalogService;

    /** JdbcTemplate sobre la BD común: saldo (f_saldo_cliente), nombre de
     *  vendedor (empleados) y chequeo de referencias para el DELETE seguro. */
    private final JdbcTemplate jdbc;

    public CustomerService(@Qualifier("commonDataSource") DataSource commonDS) {
        this.jdbc = new JdbcTemplate(commonDS);
    }

    // US-019: devuelve DTO de salida; 404 si no existe (via GlobalExceptionHandler).
    public CustomerResponse getById(int id) {
        Customer customer = customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        return enrich(List.of(customerMapper.toResponse(customer))).get(0);
    }

    // US-019: busqueda paginada de los clientes del vendedor autenticado.
    // Se enriquece con saldo CxC (f_saldo_cliente) y nombre del vendedor.
    public Page<CustomerResponse> search(String name, String user, Pageable pageable) {
        Page<CustomerResponse> page = customerRepository.search(name, user, pageable)
                .map(customerMapper::toResponse);
        return new PageImpl<>(enrich(page.getContent()), pageable, page.getTotalElements());
    }

    /** Rellena saldo (f_saldo_cliente) y vendedorNombre (empleados) en bloque. */
    private List<CustomerResponse> enrich(List<CustomerResponse> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        List<Integer> ids = rows.stream().map(CustomerResponse::id)
                .filter(java.util.Objects::nonNull).toList();
        List<Integer> vendIds = rows.stream().map(CustomerResponse::idVendedor)
                .filter(v -> v != null && v > 0).distinct().toList();
        Map<Integer, BigDecimal> saldos = saldoByCliente(ids);
        Map<Integer, String> vendedores = vendedorNombres(vendIds);
        return rows.stream().map(r -> new CustomerResponse(
                r.id(), r.name(), r.rtn(), r.address(), r.phone(), r.mobile(),
                r.tipoCliente(), r.limiteCredito(), r.idVendedor(),
                r.idVendedor() != null ? vendedores.get(r.idVendedor()) : null,
                saldos.getOrDefault(r.id(), BigDecimal.ZERO)
        )).toList();
    }

    private Map<Integer, BigDecimal> saldoByCliente(List<Integer> ids) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT codigo_cliente, ifnull(f_saldo_cliente(codigo_cliente), 0) AS s "
                        + "FROM cliente WHERE codigo_cliente IN (" + in + ")",
                rs -> { map.put(rs.getInt("codigo_cliente"), rs.getBigDecimal("s")); },
                ids.toArray());
        return map;
    }

    private Map<Integer, String> vendedorNombres(List<Integer> ids) {
        Map<Integer, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT codigo_empleado, CONCAT_WS(' ', nombre, apellido) AS n "
                        + "FROM empleados WHERE codigo_empleado IN (" + in + ")",
                rs -> { map.put(rs.getInt("codigo_empleado"), rs.getString("n")); },
                ids.toArray());
        return map;
    }

    // US-019: crea un cliente asociado al vendedor autenticado.
    // tipoCliente null = legacy del panel admin (tipo 2, solo ADMIN);
    // 1 = contado (alta rápida POS); 2 = crédito (form completo, gated por
    // config crear_cliente_credito o rol ADMIN).
    public CustomerResponse create(CustomerCreateRequest request, String user, boolean isAdmin) {
        Integer tipo = request.tipoCliente();
        if (tipo != null && tipo != 1 && tipo != 2) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "tipoCliente debe ser 1 (contado) o 2 (crédito)");
        }
        if (tipo == null && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Indique tipoCliente (1 contado / 2 crédito)");
        }
        if (tipo != null && tipo == 2) {
            if (!isAdmin && !sellerCatalogService.puedeCrearClienteCredito(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Crear clientes de crédito está bloqueado para el cajero (configuración)");
            }
            if (request.phone() == null || request.phone().isBlank()
                    || request.address() == null || request.address().isBlank()
                    || request.limiteCredito() == null
                    || request.limiteCredito().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El cliente de crédito requiere teléfono, dirección y límite de crédito mayor que 0");
            }
        }
        Customer toCreate = customerMapper.fromCreateRequest(request);
        if (tipo != null && tipo == 1) toCreate.setLimiteCredito(java.math.BigDecimal.ZERO);
        // Vendedor del usuario; si no hay empleado marcado (cajero POS), el
        // default 1 — fiel a registrarClienteContado del Swing (id_vendedor=1).
        int sellerId = sellerService.findByUser(user).map(Seller::getId).orElse(1);
        Customer created = customerRepository.create(toCreate, sellerId);
        return enrich(List.of(customerMapper.toResponse(created))).get(0);
    }

    // US-019: actualiza un cliente existente (404 si no existe).
    public CustomerResponse update(int id, CustomerCreateRequest request) {
        customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        Customer toUpdate = customerMapper.fromCreateRequest(request);
        return enrich(List.of(customerMapper.toResponse(customerRepository.update(id, toUpdate)))).get(0);
    }

    // US-019: elimina un cliente existente (404 si no existe). US Clientes:
    // 409 si tiene saldo CxC o movimientos en cuentas_por_cobrar.
    public void delete(int id) {
        customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        if (enUso(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cliente tiene saldo o movimientos de cuentas por cobrar; no se puede eliminar.");
        }
        try {
            customerRepository.delete(id);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cliente tiene referencias que impiden eliminarlo.");
        }
    }

    /** ¿El cliente tiene saldo CxC o movimientos en cuentas_por_cobrar? */
    private boolean enUso(int id) {
        Integer movs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cuentas_por_cobrar WHERE codigo_cliente = ?", Integer.class, id);
        if (movs != null && movs > 0) {
            return true;
        }
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT ifnull(f_saldo_cliente(?), 0)", BigDecimal.class, id);
        return saldo != null && saldo.signum() > 0;
    }

    private Seller resolveSeller(String user) {
        return sellerService.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vendedor no encontrado para el usuario autenticado"));
    }
}

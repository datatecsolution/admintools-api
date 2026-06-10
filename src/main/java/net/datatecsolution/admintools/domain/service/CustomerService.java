package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Customer;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.repository.CustomerRepository;
import net.datatecsolution.admintools.persistence.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Service de clientes. Reemplaza la antigua {@code CostomerService}
 * (typo historico). Conserva las dos puertas de entrada:
 *  - metodos getdAll/getdByName que usaba el legacy /costomers (controller borrado en US-022)
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

    public List<Customer> getdAll() {
        return customerRepository.getAll();
    }

    public Optional<List<Customer>> getdByName(String name, String user) {
        return customerRepository.getByNameAndUser(name, user);
    }

    // US-019: devuelve DTO de salida; 404 si no existe (via GlobalExceptionHandler).
    public CustomerResponse getById(int id) {
        Customer customer = customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        return customerMapper.toResponse(customer);
    }

    // US-019: busqueda paginada de los clientes del vendedor autenticado.
    public Page<CustomerResponse> search(String name, String user, Pageable pageable) {
        return customerRepository.search(name, user, pageable).map(customerMapper::toResponse);
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
        return customerMapper.toResponse(created);
    }

    // US-019: actualiza un cliente existente (404 si no existe).
    public CustomerResponse update(int id, CustomerCreateRequest request) {
        customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        Customer toUpdate = customerMapper.fromCreateRequest(request);
        return customerMapper.toResponse(customerRepository.update(id, toUpdate));
    }

    // US-019: elimina un cliente existente (404 si no existe).
    public void delete(int id) {
        customerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        customerRepository.delete(id);
    }

    private Seller resolveSeller(String user) {
        return sellerService.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vendedor no encontrado para el usuario autenticado"));
    }
}

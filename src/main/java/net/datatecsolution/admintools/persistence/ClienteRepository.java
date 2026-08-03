package net.datatecsolution.admintools.persistence;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Customer;
import net.datatecsolution.admintools.domain.repository.CustomerRepository;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.mapper.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository implements CustomerRepository {

    @Autowired
    private ClienteCRUD clienteCRUD;

    @Autowired
    private CustomerMapper mapper;

    @Override
    public Optional<Customer> getById(int id) {
        return clienteCRUD.findById(id).map(mapper::toCustomer);
    }

    @Override
    public Page<Customer> search(String name, String user, Pageable pageable) {
        return clienteCRUD.searchByVendedor(name, user, pageable).map(mapper::toCustomer);
    }

    @Override
    public Customer create(Customer customer, int sellerId) {
        Cliente entity = mapper.toCliente(customer);
        // Vendedor: el asignado en el form (idVendedor>0) gana; si no, el del
        // usuario autenticado (sellerId).
        Integer asignado = customer.getIdVendedor();
        entity.setIdVendedor(asignado != null && asignado > 0 ? asignado : sellerId);
        // tipo explícito (POS: 1 contado / 2 crédito); null = legacy del panel
        // admin -> 2 (gestionado). El gate de tipo 2 vive en CustomerService.
        entity.setTipoCliente(customer.getTipoCliente() != null ? customer.getTipoCliente() : 2);
        // OJO: limiteCredito ya viene mapeado por MapStruct; Cliente.setLimiteCredito
        // SUMA (legacy) en vez de asignar, así que no debe llamarse dos veces.
        // El mapper pisa los defaults de la entidad con null (columnas NOT NULL
        // del esquema legacy): restaurar los del Swing.
        if (entity.getDireccion() == null) entity.setDireccion("NA");
        if (entity.getTelefono() == null) entity.setTelefono("NA");
        if (entity.getCelular() == null) entity.setCelular("NA");
        if (entity.getRtn() == null) entity.setRtn("CF");
        if (entity.getLimiteCredito() == null) entity.setLimiteCredito(java.math.BigDecimal.ZERO);
        return mapper.toCustomer(clienteCRUD.save(entity));
    }

    @Override
    public Customer update(int id, Customer customer) {
        // Cargamos la entidad existente y solo pisamos los campos editables del
        // DTO; preservamos lo que el form no toca (tipo, saldo, ruta, etc.).
        Cliente existing = clienteCRUD.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        existing.setNombre(customer.getCustomerName());
        existing.setRtn(customer.getCustomerRTN());
        existing.setDireccion(customer.getCustomerAdress());
        existing.setTelefono(customer.getCustomerTelephoneNumber());
        if (customer.getMobile() != null) existing.setCelular(customer.getMobile());
        // Reasignación de vendedor (solo si el form la manda).
        Integer asignado = customer.getIdVendedor();
        if (asignado != null && asignado > 0) existing.setIdVendedor(asignado);
        // Límite de crédito: Cliente.setLimiteCredito SUMA (legacy). Para ASIGNAR
        // el valor exacto, aplicamos el delta (nuevo - actual).
        if (customer.getLimiteCredito() != null) {
            java.math.BigDecimal delta = customer.getLimiteCredito().subtract(existing.getLimiteCredito());
            existing.setLimiteCredito(delta);
        }
        return mapper.toCustomer(clienteCRUD.save(existing));
    }

    @Override
    public void delete(int id) {
        clienteCRUD.deleteById(id);
    }
}

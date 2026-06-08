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
    public List<Customer> getAll() {
        List<Cliente> clientes = (List<Cliente>) clienteCRUD.findAll();
        return mapper.toCustomers(clientes);
    }

    @Override
    public Optional<List<Customer>> getByNameAndUser(String name, String user) {
        List<Cliente> clientes = clienteCRUD.findByNombreVendedorOrderByNombreAsc(name, user);
        return Optional.of(mapper.toCustomers(clientes));
    }

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
        entity.setIdVendedor(sellerId);
        entity.setTipoCliente(2);   // gestionado: solo estos se listan/editan en admin y reciben crédito
        return mapper.toCustomer(clienteCRUD.save(entity));
    }

    @Override
    public Customer update(int id, Customer customer) {
        // Cargamos la entidad existente y solo pisamos los campos editables del
        // DTO; asi preservamos vendedor, limite de credito, tipo, etc.
        Cliente existing = clienteCRUD.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        existing.setNombre(customer.getCustomerName());
        existing.setRtn(customer.getCustomerRTN());
        existing.setDireccion(customer.getCustomerAdress());
        existing.setTelefono(customer.getCustomerTelephoneNumber());
        return mapper.toCustomer(clienteCRUD.save(existing));
    }

    @Override
    public void delete(int id) {
        clienteCRUD.deleteById(id);
    }
}

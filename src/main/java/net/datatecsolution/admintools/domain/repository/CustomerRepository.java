package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a clientes a nivel dominio. Reemplaza la antigua {@code CostomerRepository}
 * (typo historico). Implementacion: {@code ClienteRepository}.
 */
public interface CustomerRepository {
    List<Customer> getAll();
    Optional<List<Customer>> getByNameAndUser(String name, String user);
    Optional<Customer> getById(int id);

    // US-019
    Page<Customer> search(String name, String user, Pageable pageable);
    Customer create(Customer customer, int sellerId);
    Customer update(int id, Customer customer);
    void delete(int id);
}

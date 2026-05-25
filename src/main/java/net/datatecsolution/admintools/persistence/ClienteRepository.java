package net.datatecsolution.admintools.persistence;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.mapper.CostomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository implements CostomerRepository {
    @Autowired
    private ClienteCRUD clienteCRUD;

    @Autowired
    private CostomerMapper mapper;

    @Override
    public List<Costomer> getAll() {
        List<Cliente> clientes = (List<Cliente>) clienteCRUD.findAll();
        return mapper.toCostomers(clientes);
    }

    @Override
    public Optional<List<Costomer>> getByNameAndUser(String name, String user) {
       // List<Cliente> clientes= clienteCRUD.findByNombreContainingOrderByNombreAsc(name);
        List<Cliente> clientes= clienteCRUD.findByNombreVendedorOrderByNombreAsc(name, user);
        return Optional.of(mapper.toCostomers(clientes));
    }

    @Override
    public Optional<Costomer> getById(int id) {
        return clienteCRUD.findById(id).map(mapper::toCostomer);
    }

    @Override
    public Page<Costomer> search(String name, String user, Pageable pageable) {
        return clienteCRUD.searchByVendedor(name, user, pageable).map(mapper::toCostomer);
    }

    @Override
    public Costomer create(Costomer costomer, int sellerId) {
        Cliente entity = mapper.toCliente(costomer);
        entity.setIdVendedor(sellerId);
        return mapper.toCostomer(clienteCRUD.save(entity));
    }

    @Override
    public Costomer update(int id, Costomer costomer) {
        // Cargamos la entidad existente y solo pisamos los campos editables del
        // DTO; asi preservamos vendedor, limite de credito, tipo, etc.
        Cliente existing = clienteCRUD.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        existing.setNombre(costomer.getCostomerName());
        existing.setRtn(costomer.getCostomerRTN());
        existing.setDireccion(costomer.getCostomerAdress());
        existing.setTelefono(costomer.getCostomerTelephoneNumber());
        return mapper.toCostomer(clienteCRUD.save(existing));
    }

    @Override
    public void delete(int id) {
        clienteCRUD.deleteById(id);
    }
}

package net.datatecsolution.admintools.persistence;

import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.crud.ClienteCRUD;
import net.datatecsolution.admintools.persistence.entity.Cliente;
import net.datatecsolution.admintools.persistence.mapper.CostomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
}

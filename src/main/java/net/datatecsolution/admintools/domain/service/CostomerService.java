package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.dto.CustomerCreateRequest;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.mapper.CostomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CostomerService {
    @Autowired
    private CostomerRepository costomerRepository;

    @Autowired
    private CostomerMapper costomerMapper;

    @Autowired
    private SellerService sellerService;

    public List<Costomer> getdAll() {
        return costomerRepository.getAll();
    }
    public Optional<List<Costomer>> getdByName(String name, String user) {
        return costomerRepository.getByNameAndUser(name, user);
    }

    // US-019: devuelve el DTO de salida; 404 si no existe (via GlobalExceptionHandler).
    public CustomerResponse getById(int id) {
        Costomer costomer = costomerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        return costomerMapper.toResponse(costomer);
    }

    // US-019: busqueda paginada de los clientes del vendedor autenticado.
    public Page<CustomerResponse> search(String name, String user, Pageable pageable) {
        return costomerRepository.search(name, user, pageable).map(costomerMapper::toResponse);
    }

    // US-019: crea un cliente asociado al vendedor autenticado.
    public CustomerResponse create(CustomerCreateRequest request, String user) {
        Seller seller = resolveSeller(user);
        Costomer toCreate = costomerMapper.fromCreateRequest(request);
        Costomer created = costomerRepository.create(toCreate, seller.getId());
        return costomerMapper.toResponse(created);
    }

    // US-019: actualiza un cliente existente (404 si no existe).
    public CustomerResponse update(int id, CustomerCreateRequest request) {
        costomerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        Costomer toUpdate = costomerMapper.fromCreateRequest(request);
        return costomerMapper.toResponse(costomerRepository.update(id, toUpdate));
    }

    // US-019: elimina un cliente existente (404 si no existe).
    public void delete(int id) {
        costomerRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente " + id + " no encontrado"));
        costomerRepository.delete(id);
    }

    private Seller resolveSeller(String user) {
        return sellerService.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vendedor no encontrado para el usuario autenticado"));
    }
}

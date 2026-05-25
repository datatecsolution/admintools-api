package net.datatecsolution.admintools.domain.service;

import jakarta.persistence.EntityNotFoundException;
import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.dto.CustomerResponse;
import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import net.datatecsolution.admintools.persistence.mapper.CostomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CostomerService {
    @Autowired
    private CostomerRepository costomerRepository;

    @Autowired
    private CostomerMapper costomerMapper;

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
}

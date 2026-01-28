package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.repository.CostomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CostomerService {
    @Autowired
    private CostomerRepository costomerRepository;

    public List<Costomer> getdAll() {
        return costomerRepository.getAll();
    }
    public Optional<List<Costomer>> getdByName(String name, String user) {
        return costomerRepository.getByNameAndUser(name, user);
    }
}

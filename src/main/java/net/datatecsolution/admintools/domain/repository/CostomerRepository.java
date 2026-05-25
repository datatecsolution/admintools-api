package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Costomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CostomerRepository {
    List<Costomer> getAll();
    Optional<List<Costomer>> getByNameAndUser(String name, String user);
    Optional<Costomer> getById(int id);

    // US-019
    Page<Costomer> search(String name, String user, Pageable pageable);
    Costomer create(Costomer costomer, int sellerId);
    Costomer update(int id, Costomer costomer);
    void delete(int id);
}

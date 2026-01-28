package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Costomer;

import java.util.List;
import java.util.Optional;

public interface CostomerRepository {
    List<Costomer> getAll();
    Optional<List<Costomer>> getByNameAndUser(String name, String user);
}

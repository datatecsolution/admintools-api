package net.datatecsolution.admintools.domain.repository;

import net.datatecsolution.admintools.domain.Seller;

import java.util.Optional;

public interface SellerRepository {
    Optional<Seller> findById(Long id);
    Optional<Seller> findByUser(String user);
}

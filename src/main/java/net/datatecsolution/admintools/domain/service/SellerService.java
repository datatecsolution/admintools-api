package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SellerService {

    private static final Logger log = LoggerFactory.getLogger(SellerService.class);

    @Autowired
    private SellerRepository sellerRepository;

    public Optional<Seller> findByUser(String user) {

        Optional<Seller> seller = sellerRepository.findByUser(user);

        seller.ifPresent(s -> log.debug("Seller encontrado para user={}: {}", user, s.getName()));

        return seller;
    }
}

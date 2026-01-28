package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.domain.Seller;
import net.datatecsolution.admintools.domain.repository.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SellerService {

    @Autowired
    private SellerRepository sellerRepository;

    public Optional<Seller> findByUser(String user) {

        Optional<Seller> seller= sellerRepository.findByUser(user);

        System.out.println("El nombre del usuario es ????======>>>>>>>>"+seller.get().getName());

        return seller;
    }
}

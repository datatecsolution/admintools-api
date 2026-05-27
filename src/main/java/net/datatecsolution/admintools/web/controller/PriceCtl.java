package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Price;
import net.datatecsolution.admintools.domain.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/price")
public class PriceCtl {
    @Autowired
    private PriceService priceService;

    @GetMapping("/all")
    public List<Price> getAll() {
        return priceService.getAll();
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")   // US-021
    public ResponseEntity<Price> save(@RequestBody Price price) {
        return new ResponseEntity<>(priceService.save(price), HttpStatus.CREATED);
    }
}

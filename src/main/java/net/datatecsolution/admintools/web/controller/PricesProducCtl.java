package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.PriceProduct;
import net.datatecsolution.admintools.domain.service.PricesProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/prices")
public class PricesProducCtl {
    @Autowired
    private PricesProductService pricesProductService;

    @GetMapping("/all")
    public List<PriceProduct> getAll() {
        return pricesProductService.getAll();
    }
}

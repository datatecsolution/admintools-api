package net.datatecsolution.admintools.domain;

import java.util.List;

public class Price {

    private Integer priceId;
    private String description;

    private List<PriceProduct> priceProducts;

    public List<PriceProduct> getPriceProducts() {
        return priceProducts;
    }

    public void setPriceProducts(List<PriceProduct> priceProducts) {
        this.priceProducts = priceProducts;
    }

    public Integer getPriceId() {
        return priceId;
    }

    public void setPriceId(Integer priceId) {
        this.priceId = priceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}

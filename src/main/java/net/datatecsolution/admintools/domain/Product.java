package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class Product {
    private Integer productId;
    private String name;
    private Integer categoryId;
    private Integer taxId;
    private Boolean active;
    private BigDecimal price;
    private Category category;

    private Double stock;
    private Tax tax;
    private List<PriceProduct> prices;

    private Set<Price> prics;

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getTaxId() {
        return taxId;
    }

    public void setTaxId(Integer taxId) {
        this.taxId = taxId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Tax getTax() {
        return tax;
    }

    public void setTax(Tax tax) {
        this.tax = tax;
    }

    public List<PriceProduct> getPrices() {
        return prices;
    }

    public void setPrices(List<PriceProduct> prices) {
        this.prices = prices;
    }
    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }
    public Set<Price> getPrics() {
        return prics;
    }

    public void setPrics(Set<Price> prics) {
        this.prics = prics;
    }
}

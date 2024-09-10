package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;

public class PriceProduct {


    private Integer priceProductId;
    private BigDecimal priceProduct;
    private Product product;

    private  Price price;

    private Integer priceId;
    private Integer productId;

    public Integer getPriceProductId() {
        return priceProductId;
    }

    public void setPriceProductId(Integer priceProductId) {
        this.priceProductId = priceProductId;
    }

    public BigDecimal getPriceProduct() {
        return priceProduct;
    }

    public void setPriceProduct(BigDecimal priceProduct) {
        this.priceProduct = priceProduct;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public Integer getPriceId() {
        return priceId;
    }

    public void setPriceId(Integer priceId) {
        this.priceId = priceId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }
}

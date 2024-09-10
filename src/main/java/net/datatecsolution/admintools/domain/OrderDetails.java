package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;

public class OrderDetails {
    public Integer getDetailId() {
        return detailId;
    }

    public void setDetailId(Integer detailId) {
        this.detailId = detailId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getDiscountItem() {
        return discountItem;
    }

    public void setDiscountItem(BigDecimal discountItem) {
        this.discountItem = discountItem;
    }

    public Double getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(Double priceItem) {
        this.priceItem = priceItem;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getPriceItemId() {
        return priceItemId;
    }

    public void setPriceItemId(Integer priceItemId) {
        this.priceItemId = priceItemId;
    }

    private Integer detailId;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal subTotal;
    private BigDecimal discountItem;
    private Double priceItem;
    private int productId;
    private double discount;
    private int orderId;
    private Product product;
    private Integer priceItemId;

}

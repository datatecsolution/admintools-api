package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Order {
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getSubTotalExcento() {
        return subTotalExcento;
    }

    public void setSubTotalExcento(BigDecimal subTotalExcento) {
        this.subTotalExcento = subTotalExcento;
    }

    public BigDecimal getSubTotal15() {
        return subTotal15;
    }

    public void setSubTotal15(BigDecimal subTotal15) {
        this.subTotal15 = subTotal15;
    }

    public BigDecimal getSubTotal18() {
        return subTotal18;
    }

    public void setSubTotal18(BigDecimal subTotal18) {
        this.subTotal18 = subTotal18;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public BigDecimal getIsvOther() {
        return isvOther;
    }

    public void setIsvOther(BigDecimal isvOther) {
        this.isvOther = isvOther;
    }

    public BigDecimal getTotalTaxs18() {
        return totalTaxs18;
    }

    public void setTotalTaxs18(BigDecimal totalTaxs18) {
        this.totalTaxs18 = totalTaxs18;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    public Integer getCostomerId() {
        return costomerId;
    }

    public void setCostomerId(Integer costomerId) {
        this.costomerId = costomerId;
    }
    public Costomer getCostomer() {
        return costomer;
    }

    public void setCostomer(Costomer costomer) {
        this.costomer = costomer;
    }
    public List<OrderDetails> getDetails() {
        return details;
    }

    public void setDetails(List<OrderDetails> details) {
        this.details = details;
    }
    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }



    private Integer orderId;
    private LocalDate date;
    private BigDecimal subTotalExcento;
    private BigDecimal subTotal15;
    private BigDecimal subTotal18;
    private BigDecimal subTotal;
    private BigDecimal totalTax;
    private BigDecimal total;
    private String active;
    private BigDecimal isvOther;
    private BigDecimal totalTaxs18;
    private String user;
    private Integer costomerId;
    private Costomer costomer;
    private Integer sellerId;
    private List<OrderDetails> details;
}

package net.datatecsolution.admintools.domain;

public class UserPrice {
    public Integer getUserPriceId() {
        return userPriceId;
    }

    public void setUserPriceId(Integer userPriceId) {
        this.userPriceId = userPriceId;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public User getUs() {
        return us;
    }

    public void setUs(User us) {
        this.us = us;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPriceId() {
        return priceId;
    }

    public void setPriceId(Integer priceId) {
        this.priceId = priceId;
    }

    private Integer userPriceId;
    private Price price;
    private User us;
    private String userId;
    private Integer priceId;

}

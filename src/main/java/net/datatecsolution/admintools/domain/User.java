package net.datatecsolution.admintools.domain;

import java.util.List;

public class User {

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public List<UserPrice> getUserPrices() {
        return userPrices;
    }

    public void setUserPrices(List<UserPrice> userPrices) {
        this.userPrices = userPrices;
    }

    private Integer userId;
    private String username;
    private String password;
    private List<UserPrice> userPrices;
}

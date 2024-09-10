package net.datatecsolution.admintools.domain;

public class Costomer {

    public Integer getCostomerId() {
        return costomerId;
    }

    public void setCostomerId(Integer costomerId) {
        this.costomerId = costomerId;
    }

    public String getCostomerName() {
        return costomerName;
    }

    public void setCostomerName(String costomerName) {
        this.costomerName = costomerName;
    }

    public String getCostomerRTN() {
        return costomerRTN;
    }

    public void setCostomerRTN(String costomerRTN) {
        this.costomerRTN = costomerRTN;
    }

    public String getCostomerAdress() {
        return costomerAdress;
    }

    public void setCostomerAdress(String costomerAdress) {
        this.costomerAdress = costomerAdress;
    }

    public String getCostomerTelephoneNumber() {
        return costomerTelephoneNumber;
    }

    public void setCostomerTelephoneNumber(String costomerTelephoneNumber) {
        this.costomerTelephoneNumber = costomerTelephoneNumber;
    }

    private Integer costomerId;
    private String costomerName;
    private String costomerRTN;
    private String costomerAdress;
    private String costomerTelephoneNumber;

}

package net.datatecsolution.admintools.domain;

/**
 * POJO de dominio para clientes. Reemplaza el typo historico {@code Costomer}.
 * El JSON publico de /orders pasa {@code costomer/costomerId} -> {@code customer/customerId}
 * (US-022: breaking change coordinado con cutover del frontend).
 */
public class Customer {

    private Integer customerId;
    private String customerName;
    private String customerRTN;
    private String customerAdress;
    private String customerTelephoneNumber;

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerRTN() {
        return customerRTN;
    }

    public void setCustomerRTN(String customerRTN) {
        this.customerRTN = customerRTN;
    }

    public String getCustomerAdress() {
        return customerAdress;
    }

    public void setCustomerAdress(String customerAdress) {
        this.customerAdress = customerAdress;
    }

    public String getCustomerTelephoneNumber() {
        return customerTelephoneNumber;
    }

    public void setCustomerTelephoneNumber(String customerTelephoneNumber) {
        this.customerTelephoneNumber = customerTelephoneNumber;
    }
}

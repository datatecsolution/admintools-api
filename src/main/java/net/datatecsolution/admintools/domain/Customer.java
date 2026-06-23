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
    /** 1 = contado/rápido (oculto en admin, sin crédito); 2 = gestionado. */
    private Integer tipoCliente;
    private java.math.BigDecimal limiteCredito;
    /** Celular (columna cliente.movil). El form legacy lo omitía. */
    private String mobile;
    /** Vendedor asignado (cliente.id_vendedor). */
    private Integer idVendedor;
    /** Nombre del vendedor (join empleados) — lo llena el service. */
    private String vendedorNombre;
    /** Saldo CxC (f_saldo_cliente) — lo llena el service, no es columna directa. */
    private java.math.BigDecimal saldo;

    public java.math.BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(java.math.BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getIdVendedor() {
        return idVendedor;
    }

    public void setIdVendedor(Integer idVendedor) {
        this.idVendedor = idVendedor;
    }

    public String getVendedorNombre() {
        return vendedorNombre;
    }

    public void setVendedorNombre(String vendedorNombre) {
        this.vendedorNombre = vendedorNombre;
    }

    public java.math.BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(java.math.BigDecimal saldo) {
        this.saldo = saldo;
    }

    public Integer getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(Integer tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

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

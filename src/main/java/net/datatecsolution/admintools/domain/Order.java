package net.datatecsolution.admintools.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
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
    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    // ---------------------------------------------------------------
    // US-124 — PUENTE con el build DESPLEGADO de la app de pedidos, que
    // usa el nombre histórico `costomer*`. Se expone en AMBOS sentidos:
    //   · salida  → la lista de pedidos necesita `costomer.costomerName`
    //   · entrada → sin `costomerId` el insert falla con
    //     "Column 'codigo_cliente' cannot be null" (visto en producción
    //     2026-07-31: 11 pedidos rechazados con HTTP 500).
    // Se implementa como par getter/setter con el MISMO nombre JSON para
    // que Jackson lo trate como una sola propiedad (sin ambigüedad).
    // BORRAR cuando el front desplegado se actualice a customerId/customer.
    // ---------------------------------------------------------------

    @JsonProperty("costomerId")
    public Integer getCostomerId() {
        return customerId;
    }

    @JsonProperty("costomerId")
    public void setCostomerId(Integer costomerId) {
        if (costomerId != null) {
            this.customerId = costomerId;
        }
    }

    @JsonProperty("costomer")
    public CostomerLegacy getCostomer() {
        return CostomerLegacy.de(customer);
    }

    @JsonProperty("costomer")
    public void setCostomer(CostomerLegacy costomer) {
        if (costomer == null) {
            return;
        }
        // El id puede venir solo en el objeto: sin esto el pedido quedaría
        // sin cliente. No depende del orden de las claves del JSON.
        if (costomer.costomerId() != null) {
            this.customerId = costomer.costomerId();
        }
        if (this.customer == null) {
            this.customer = costomer.aCustomer();
        }
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

    // Método para formatear la fecha antes de enviarla al frontend
    @JsonProperty("date")
    public String getFormattedDate() {
        // Definir el patrón y el Locale para español de Honduras
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d, MMMM", new Locale("es", "HN"));

        // Devolver la fecha formateada
        return date != null ? date.format(formatter) : null;
    }

    @JsonIgnore
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Hora de la orden ("HH:mm") para las listas del POS. Campo aditivo:
     * {@code date} sigue siendo el string largo que consume la app de pedidos.
     */
    @JsonProperty("time")
    public String getFormattedTime() {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    public String getObser() {
        return obser;
    }

    public void setObser(String obser) {
        this.obser = obser;
    }



    private Integer orderId;
   // @JsonFormat(pattern = "EEEE d, MMMM", locale = "es_HN")
    private LocalDate date;
    /** fecha completa (con hora) del encabezado; expuesta como "time". */
    private LocalDateTime dateTime;
    private BigDecimal subTotalExcento;
    private BigDecimal subTotal15;
    private BigDecimal subTotal18;
    private BigDecimal subTotal;
    private BigDecimal totalTax;
    private BigDecimal total;
    private Integer active;
    private BigDecimal isvOther;
    private BigDecimal totalTaxs18;
    private String user;
    private String obser;
    private Integer customerId;
    private Customer customer;
    private Integer sellerId;
    private List<OrderDetails> details;
}

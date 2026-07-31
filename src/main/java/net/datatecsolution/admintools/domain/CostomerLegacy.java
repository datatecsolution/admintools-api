package net.datatecsolution.admintools.domain;

/**
 * US-124 — forma HISTÓRICA del cliente (con el typo `costomer`) que usa el
 * build DESPLEGADO de la app de pedidos (at-ordenes-ventas en Sharon).
 *
 * El dominio migró a {@link Customer} (`customerId`, `customerName`…), pero
 * ese front sigue leyendo y enviando `costomerId`, `costomerName`,
 * `costomerRTN`, `costomerAdress`, `costomerTelephoneNumber`.
 *
 * BORRAR junto con LegacyOrdersAppCtl y los accesores legacy de {@link Order}
 * cuando el front desplegado se actualice.
 */
public record CostomerLegacy(
        Integer costomerId,
        String costomerName,
        String costomerRTN,
        String costomerAdress,
        String costomerTelephoneNumber) {

    /** Proyecta el cliente actual a la forma histórica (null-safe). */
    public static CostomerLegacy de(Customer c) {
        if (c == null) {
            return null;
        }
        return new CostomerLegacy(
                c.getCustomerId(),
                c.getCustomerName(),
                c.getCustomerRTN(),
                c.getCustomerAdress(),
                c.getCustomerTelephoneNumber());
    }

    /** Reconstruye un {@link Customer} desde la forma histórica (null-safe). */
    public Customer aCustomer() {
        Customer c = new Customer();
        c.setCustomerId(costomerId);
        c.setCustomerName(costomerName);
        c.setCustomerRTN(costomerRTN);
        c.setCustomerAdress(costomerAdress);
        c.setCustomerTelephoneNumber(costomerTelephoneNumber);
        return c;
    }
}

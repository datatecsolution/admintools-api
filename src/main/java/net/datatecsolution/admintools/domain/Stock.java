package net.datatecsolution.admintools.domain;

import java.math.BigDecimal;

/**
 * POJO de dominio: stock actual de un articulo en una bodega en un momento
 * dado. Mapeado desde {@code ExistenciaArticuloBodega} (entity, persistence)
 * por {@code StockMapper}. Convertido a {@code StockResponse} para exponerlo
 * via API.
 */
public class Stock {

    private Integer productCode;
    private Integer warehouseCode;
    private String warehouseDescription;
    private BigDecimal quantity;

    public Integer getProductCode() { return productCode; }
    public void setProductCode(Integer productCode) { this.productCode = productCode; }

    public Integer getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(Integer warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseDescription() { return warehouseDescription; }
    public void setWarehouseDescription(String warehouseDescription) { this.warehouseDescription = warehouseDescription; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}

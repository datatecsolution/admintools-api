package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Acta de toma física (encabezado) — tabla {@code inventory_count} (V32).
 * Registro de cada inventario físico: quién contó, cuándo, en qué bodega,
 * cuántos faltantes/sobrantes/negativos y el dinero que representan. Es un
 * documento de auditoría aparte de los movimientos del cierre (requisición
 * de faltantes + compras de ajuste). Vive en la BD común (admin_tools).
 */
@Entity
@Table(name = "inventory_count")
public class InventoryCount {

    @Id
    @Column(name = "codigo_inventario_count")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigoInventarioCount;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "codigo_bodega", nullable = false)
    private Integer codigoBodega;

    @Column(name = "contadas", nullable = false)
    private Integer contadas;

    @Column(name = "faltantes", nullable = false)
    private Integer faltantes;

    @Column(name = "sobrantes", nullable = false)
    private Integer sobrantes;

    @Column(name = "negativos", nullable = false)
    private Integer negativos;

    @Column(name = "valor_ajuste", nullable = false)
    private BigDecimal valorAjuste;

    @Column(name = "valor_negativos", nullable = false)
    private BigDecimal valorNegativos;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    public Integer getCodigoInventarioCount() { return codigoInventarioCount; }
    public void setCodigoInventarioCount(Integer v) { this.codigoInventarioCount = v; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime v) { this.fecha = v; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String v) { this.usuario = v; }
    public Integer getCodigoBodega() { return codigoBodega; }
    public void setCodigoBodega(Integer v) { this.codigoBodega = v; }
    public Integer getContadas() { return contadas; }
    public void setContadas(Integer v) { this.contadas = v; }
    public Integer getFaltantes() { return faltantes; }
    public void setFaltantes(Integer v) { this.faltantes = v; }
    public Integer getSobrantes() { return sobrantes; }
    public void setSobrantes(Integer v) { this.sobrantes = v; }
    public Integer getNegativos() { return negativos; }
    public void setNegativos(Integer v) { this.negativos = v; }
    public BigDecimal getValorAjuste() { return valorAjuste; }
    public void setValorAjuste(BigDecimal v) { this.valorAjuste = v; }
    public BigDecimal getValorNegativos() { return valorNegativos; }
    public void setValorNegativos(BigDecimal v) { this.valorNegativos = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { this.motivo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
}

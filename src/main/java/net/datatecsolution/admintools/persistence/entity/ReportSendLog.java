package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * US-068 — histórico de envíos ({@code report_send_log}, V49). Un registro
 * por intento de ciclo: estado OK|FALLO con intentos y detalle del error.
 * Para origen AUTO, (schedule_id, fecha_programada) es la clave de
 * idempotencia del scheduler.
 */
@Entity
@Table(name = "report_send_log")
public class ReportSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    @Column(name = "fecha_envio", insertable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "origen")
    private String origen = "AUTO";

    @Column(name = "estado")
    private String estado;

    @Column(name = "intentos")
    private Integer intentos = 1;

    @Column(name = "detalle")
    private String detalle;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getScheduleId() { return scheduleId; }
    public void setScheduleId(Integer scheduleId) { this.scheduleId = scheduleId; }
    public LocalDate getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(LocalDate fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}

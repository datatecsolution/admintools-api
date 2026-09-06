package net.datatecsolution.admintools.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * US-068 — programación de envío de un reporte por correo (tabla
 * {@code report_schedules}, V49). reporte: DAILY|ABC|ROTATION|PURCHASE|
 * CATEGORY; frecuencia: DIARIA|SEMANAL (con dia_semana 1=lunes..7=domingo).
 */
@Entity
@Table(name = "report_schedules")
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reporte")
    private String reporte;

    @Column(name = "frecuencia")
    private String frecuencia;

    @Column(name = "dia_semana")
    private Integer diaSemana;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "destinatarios")
    private String destinatarios;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "creado", insertable = false, updatable = false)
    private LocalDateTime creado;

    @Column(name = "actualizado", insertable = false, updatable = false)
    private LocalDateTime actualizado;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getReporte() { return reporte; }
    public void setReporte(String reporte) { this.reporte = reporte; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(String destinatarios) { this.destinatarios = destinatarios; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getActualizado() { return actualizado; }
}

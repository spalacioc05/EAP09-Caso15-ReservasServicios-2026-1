package com.eap09.reservas.provideroffer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tbl_servicio")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @Column(name = "id_usuario_proveedor", nullable = false)
    private Long idUsuarioProveedor;

    @Column(name = "id_estado_servicio", nullable = false)
    private Long idEstadoServicio;

    @Column(name = "nombre_servicio", nullable = false)
    private String nombreServicio;

    @Column(name = "descripcion_servicio", nullable = false)
    private String descripcionServicio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "capacidad_maxima_concurrente", nullable = false)
    private Integer capacidadMaximaConcurrente;

    @Column(name = "fecha_creacion_servicio", nullable = false)
    private OffsetDateTime fechaCreacionServicio;

    @Column(name = "fecha_actualizacion_servicio", nullable = false)
    private OffsetDateTime fechaActualizacionServicio;

    public Long getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(Long idServicio) {
        this.idServicio = idServicio;
    }

    public Long getIdUsuarioProveedor() {
        return idUsuarioProveedor;
    }

    public void setIdUsuarioProveedor(Long idUsuarioProveedor) {
        this.idUsuarioProveedor = idUsuarioProveedor;
    }

    public Long getIdEstadoServicio() {
        return idEstadoServicio;
    }

    public void setIdEstadoServicio(Long idEstadoServicio) {
        this.idEstadoServicio = idEstadoServicio;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public String getDescripcionServicio() {
        return descripcionServicio;
    }

    public void setDescripcionServicio(String descripcionServicio) {
        this.descripcionServicio = descripcionServicio;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getCapacidadMaximaConcurrente() {
        return capacidadMaximaConcurrente;
    }

    public void setCapacidadMaximaConcurrente(Integer capacidadMaximaConcurrente) {
        this.capacidadMaximaConcurrente = capacidadMaximaConcurrente;
    }

    public OffsetDateTime getFechaCreacionServicio() {
        return fechaCreacionServicio;
    }

    public void setFechaCreacionServicio(OffsetDateTime fechaCreacionServicio) {
        this.fechaCreacionServicio = fechaCreacionServicio;
    }

    public OffsetDateTime getFechaActualizacionServicio() {
        return fechaActualizacionServicio;
    }

    public void setFechaActualizacionServicio(OffsetDateTime fechaActualizacionServicio) {
        this.fechaActualizacionServicio = fechaActualizacionServicio;
    }
}
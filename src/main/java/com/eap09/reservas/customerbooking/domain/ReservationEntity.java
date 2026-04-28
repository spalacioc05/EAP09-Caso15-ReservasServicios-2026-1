package com.eap09.reservas.customerbooking.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tbl_reserva")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    @Column(name = "id_disponibilidad_servicio", nullable = false)
    private Long idDisponibilidadServicio;

    @Column(name = "id_usuario_cliente", nullable = false)
    private Long idUsuarioCliente;

    @Column(name = "id_estado_reserva", nullable = false)
    private Long idEstadoReserva;

    @Column(name = "fecha_creacion_reserva", nullable = false)
    private OffsetDateTime fechaCreacionReserva;

    @Column(name = "fecha_actualizacion_reserva")
    private OffsetDateTime fechaActualizacionReserva;

    @Column(name = "fecha_cancelacion_reserva")
    private OffsetDateTime fechaCancelacionReserva;

    @Column(name = "fecha_finalizacion_reserva")
    private OffsetDateTime fechaFinalizacionReserva;

    // getters y setters

    public Long getIdReserva() {
        return idReserva;
    }

    public Long getIdDisponibilidadServicio() {
        return idDisponibilidadServicio;
    }

    public void setIdDisponibilidadServicio(Long idDisponibilidadServicio) {
        this.idDisponibilidadServicio = idDisponibilidadServicio;
    }

    public Long getIdUsuarioCliente() {
        return idUsuarioCliente;
    }

    public void setIdUsuarioCliente(Long idUsuarioCliente) {
        this.idUsuarioCliente = idUsuarioCliente;
    }

    public Long getIdEstadoReserva() {
        return idEstadoReserva;
    }

    public void setIdEstadoReserva(Long idEstadoReserva) {
        this.idEstadoReserva = idEstadoReserva;
    }

    public OffsetDateTime getFechaCreacionReserva() {
        return fechaCreacionReserva;
    }

    public void setFechaCreacionReserva(OffsetDateTime fechaCreacionReserva) {
        this.fechaCreacionReserva = fechaCreacionReserva;
    }

    public OffsetDateTime getFechaActualizacionReserva() {
        return fechaActualizacionReserva;
    }

    public void setFechaActualizacionReserva(OffsetDateTime fechaActualizacionReserva) {
        this.fechaActualizacionReserva = fechaActualizacionReserva;
    }

    public OffsetDateTime getFechaCancelacionReserva() {
        return fechaCancelacionReserva;
    }

    public void setFechaCancelacionReserva(OffsetDateTime fechaCancelacionReserva) {
        this.fechaCancelacionReserva = fechaCancelacionReserva;
    }

    public OffsetDateTime getFechaFinalizacionReserva() {
        return fechaFinalizacionReserva;
    }

    public void setFechaFinalizacionReserva(OffsetDateTime fechaFinalizacionReserva) {
        this.fechaFinalizacionReserva = fechaFinalizacionReserva;
    }
}
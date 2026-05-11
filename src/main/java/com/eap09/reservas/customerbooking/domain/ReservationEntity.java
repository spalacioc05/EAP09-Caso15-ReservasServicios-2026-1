package com.eap09.reservas.customerbooking.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

}
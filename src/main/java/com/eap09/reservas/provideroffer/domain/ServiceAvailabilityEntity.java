package com.eap09.reservas.provideroffer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tbl_disponibilidad_servicio")
public class ServiceAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidad_servicio")
    private Long idDisponibilidadServicio;

    @Column(name = "id_servicio", nullable = false)
    private Long idServicio;

    @Column(name = "id_estado_disponibilidad", nullable = false)
    private Long idEstadoDisponibilidad;

    @Column(name = "fecha_disponibilidad", nullable = false)
    private LocalDate fechaDisponibilidad;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_creacion_disponibilidad", nullable = false)
    private OffsetDateTime fechaCreacionDisponibilidad;

    @Column(name = "fecha_actualizacion_disponibilidad", nullable = false)
    private OffsetDateTime fechaActualizacionDisponibilidad;
}
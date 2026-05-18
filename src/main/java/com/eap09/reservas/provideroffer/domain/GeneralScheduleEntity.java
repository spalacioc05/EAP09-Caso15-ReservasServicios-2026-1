package com.eap09.reservas.provideroffer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tbl_horario_general_proveedor")
public class GeneralScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_general_proveedor")
    private Long idHorarioGeneralProveedor;

    @Column(name = "id_usuario_proveedor", nullable = false)
    private Long idUsuarioProveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dia_semana", nullable = false)
    private WeekDayEntity diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_creacion_horario_general", nullable = false)
    private OffsetDateTime fechaCreacionHorarioGeneral;

    @Column(name = "fecha_actualizacion_horario_general", nullable = false)
    private OffsetDateTime fechaActualizacionHorarioGeneral;

}

package com.eap09.reservas.provideroffer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tbl_dia_semana")
public class WeekDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dia_semana")
    private Long idDiaSemana;

    @Column(name = "nombre_dia_semana", nullable = false)
    private String nombreDiaSemana;

    @Column(name = "orden_dia_semana", nullable = false)
    private Integer ordenDiaSemana;
}

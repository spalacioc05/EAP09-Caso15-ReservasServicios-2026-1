package com.eap09.reservas.provideroffer.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ServiceAvailabilityResponse(
        Long idDisponibilidad,
        Long serviceId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estadoDisponibilidad
) {
}
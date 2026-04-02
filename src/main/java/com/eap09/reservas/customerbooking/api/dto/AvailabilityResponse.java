package com.eap09.reservas.customerbooking.api.dto;

import java.time.LocalTime;

public record AvailabilityResponse(
        Long idDisponibilidadServicio,
        LocalTime horaInicio,
        LocalTime horaFin,
        long cuposDisponibles
) {
}
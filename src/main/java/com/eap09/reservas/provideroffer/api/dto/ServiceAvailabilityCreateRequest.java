package com.eap09.reservas.provideroffer.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ServiceAvailabilityCreateRequest(
        @NotNull(message = "fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "horaInicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "horaFin es obligatoria")
        LocalTime horaFin
) {
}
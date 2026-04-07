package com.eap09.reservas.provideroffer.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

public record ServiceAvailabilityResponse(
        Long idDisponibilidad,
        Long serviceId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(type = "string", example = "2026-04-20")
        LocalDate fecha,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "09:00:00")
        LocalTime horaInicio,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "10:00:00")
        LocalTime horaFin,
        String estadoDisponibilidad
) {
}
package com.eap09.reservas.provideroffer.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ServiceAvailabilityCreateRequest(
        @NotNull(message = "fecha es obligatoria")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(type = "string", example = "2026-04-20")
        LocalDate fecha,

        @NotNull(message = "horaInicio es obligatoria")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "09:00:00")
        LocalTime horaInicio,

        @NotNull(message = "horaFin es obligatoria")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "10:00:00")
        LocalTime horaFin
) {
}
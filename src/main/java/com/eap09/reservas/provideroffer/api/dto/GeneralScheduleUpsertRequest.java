package com.eap09.reservas.provideroffer.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record GeneralScheduleUpsertRequest(
        @NotNull(message = "horaInicio es obligatoria")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "08:00:00")
        LocalTime horaInicio,

        @NotNull(message = "horaFin es obligatoria")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "12:00:00")
        LocalTime horaFin
) {
}

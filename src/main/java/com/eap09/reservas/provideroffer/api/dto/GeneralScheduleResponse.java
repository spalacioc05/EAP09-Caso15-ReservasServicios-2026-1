package com.eap09.reservas.provideroffer.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record GeneralScheduleResponse(
        Long providerUserId,
        String dayOfWeek,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "08:00:00")
        LocalTime horaInicio,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "12:00:00")
        LocalTime horaFin
) {
}

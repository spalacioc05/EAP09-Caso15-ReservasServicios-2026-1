package com.eap09.reservas.customerbooking.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long availabilityId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "09:00:00")
        LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
        @Schema(type = "string", example = "10:00:00")
        LocalTime endTime,
        long remainingSlots
) {
}
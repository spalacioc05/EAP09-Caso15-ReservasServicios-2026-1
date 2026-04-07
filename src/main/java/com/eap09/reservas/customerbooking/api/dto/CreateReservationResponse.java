package com.eap09.reservas.customerbooking.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CreateReservationResponse(

        Long bookingId,
        Long providerId,
        Long serviceId,
        Long availabilityId,
        Long customerId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(type = "string", example = "2026-04-20")
        LocalDate slotDate,
        String bookingStatus,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        @Schema(type = "string", example = "2026-04-20T09:30:00-05:00")
        OffsetDateTime createdAt

) {
}